package ru.yandex.qatools.processors.matcher.gen;

import com.squareup.javapoet.JavaFile;
import ru.yandex.qatools.processors.matcher.gen.bean.ClassSpecDescription;
import ru.yandex.qatools.processors.matcher.gen.processing.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.groupingBy;
import static ru.yandex.qatools.processors.matcher.gen.MatchersGenProperties.props;
import static ru.yandex.qatools.processors.matcher.gen.processing.MethodsCollector.collectingMethods;
import static ru.yandex.qatools.processors.matcher.gen.processing.ProcessingPredicates.isEntryWithParentPackageElement;

/**
 * @author lanwen (Merkushev Kirill)
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MatcherFactoryGenerator extends AbstractProcessor {

    private static final Logger LOGGER = Logger.getLogger(MatcherFactoryGenerator.class.toString());

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(props().annotationsToProcess());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {

            if (roundEnv.processingOver()) {
                return false;
            }

            if (annotations.isEmpty()) {
                LOGGER.info("No any annotation found...");
                return false;
            }

            List<Element> properties = new LinkedList<>();

            for (TypeElement annotation : annotations) {
                LOGGER.info(format("Work with %s...", annotation.getQualifiedName()));
                roundEnv.getElementsAnnotatedWith(annotation)
                        .stream()
                        .flatMap(MatcherFactoryGenerator::asProperty)
                        .filter(ProcessingPredicates.shouldGenerateMatcher())
                        .forEach(properties::add);
            }

            Map<Element, ClassSpecDescription> classes = groupedClasses(properties);

            LOGGER.info(format("Got %s classes to generate matchers. Writing them...", classes.size()));

            classes.values()
                    .stream()
                    .filter(isEntryWithParentPackageElement())
                    .map(ClassSpecDescription::asJavaFile)
                    .forEach(write(processingEnv));

            LOGGER.info("All classes were successfully processed!");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, format("Can't generate matchers, because of: %s", e.getMessage()), e);
        }

        return false;
    }

    static Map<Element, ClassSpecDescription> groupedClasses(List<Element> properties)
    {
        Map<Element, ClassSpecDescription> classes = properties.stream()
                .collect(groupingBy(
                        Element::getEnclosingElement,
                        collectingMethods()
                ));

        classes.values()
                .stream()
                .filter(isEntryWithParentPackageElement().negate())
                .sorted(reverseOrder())
                .forEach(spec -> spec.mergeWithParentFrom(classes));
        return classes;
    }

    /**
     * Consumer which writes classes as java files
     */
    private static Consumer<JavaFile> write(ProcessingEnvironment processingEnv) {
        return file -> {
            try {
                file.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                JavaFileObject obj = file.toJavaFileObject();
                throw new ProcessingException("Can't write", obj.getName(), obj.getKind(), e);
            }
        };
    }

    /**
     * Recursively adds to stream all properties of class of nested class
     *
     * @param element class/property element
     * @return stream with properties we want to process
     */
    private static Stream<Element> asProperty(Element element)
    {
        switch (element.getKind()) {
            case FIELD:
            case METHOD:
                return Stream.of(element);
            case CLASS:
                return element.getEnclosedElements().stream().flatMap(MatcherFactoryGenerator::asProperty);
        }
        return Stream.empty();
    }
}
