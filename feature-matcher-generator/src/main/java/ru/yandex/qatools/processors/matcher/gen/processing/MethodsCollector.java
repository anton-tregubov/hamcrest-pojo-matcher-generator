package ru.yandex.qatools.processors.matcher.gen.processing;

import com.squareup.javapoet.*;
import org.hamcrest.*;
import ru.yandex.qatools.processors.matcher.gen.MatcherFactoryGenerator;
import ru.yandex.qatools.processors.matcher.gen.bean.ClassSpecDescription;

import javax.annotation.Generated;
import javax.lang.model.element.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

import static ru.yandex.qatools.processors.matcher.gen.processing.Naming.withGeneratedSuffix;
import static ru.yandex.qatools.processors.matcher.gen.processing.ProcessingPredicates.hasParentPackageElement;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class MethodsCollector implements Collector<Element, LinkedList<Element>, ClassSpecDescription> {
    private MethodsCollector() {
        // for readability use factory
    }

    public static MethodsCollector collectingMethods() {
        return new MethodsCollector();
    }

    @Override
    public Supplier<LinkedList<Element>> supplier() {
        return LinkedList::new;
    }

    @Override
    public BiConsumer<LinkedList<Element>, Element> accumulator() {
        return LinkedList::add;
    }

    @Override
    public BinaryOperator<LinkedList<Element>> combiner() {
        return (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        };
    }

    @Override
    public Function<LinkedList<Element>, ClassSpecDescription> finisher() {
        return (LinkedList<Element> collected) -> {
            Element classOfProperty = collected.getFirst().getEnclosingElement();

            TypeSpec.Builder builder = commonClassPart(classOfProperty);
            collected.stream().map(asMethodSpec()).forEach(builder::addMethod);

            return new ClassSpecDescription(classOfProperty, builder.build());
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(Collections.singleton(Characteristics.UNORDERED));
    }


    public static TypeSpec.Builder commonClassPart(Element classOfProperty)
    {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(withGeneratedSuffix(classOfProperty.getSimpleName()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addJavadoc("You should not instantiate this class\n")
                                .addStatement(
                                        "throw new $T($S)",
                                        UnsupportedOperationException.class,
                                        "This class has only static methods"
                                )
                                .build()
                );

        if (hasParentPackageElement().test(classOfProperty))
        {
            builder.addAnnotation(
                    AnnotationSpec.builder(Generated.class)
                            .addMember("value", "$S", MatcherFactoryGenerator.class.getCanonicalName())
                            .addMember("date", "$S", ZonedDateTime.now()).build()
            );
        } else {
            builder.addModifiers(Modifier.STATIC);
        }
        return builder;
    }


    public static Function<Element, MethodSpec> asMethodSpec() {
        return property ->
        {
            boolean isGetter = property instanceof ExecutableElement;
            TypeName propertyType = TypeName.get(isGetter ? ((ExecutableElement)property).getReturnType() : property.asType()).box();
            TypeName ownerType = TypeName.get(property.getEnclosingElement().asType());
            ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(Matcher.class), ownerType);
            ParameterSpec matcher = ParameterSpec.builder(
                    ParameterizedTypeName.get(ClassName.get(Matcher.class), propertyType),
                    "matcher"
            ).build();

            return MethodSpec.methodBuilder("with" + Naming.normalize(property.getSimpleName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(matcher)
                    .returns(returnType)
                    .addJavadoc("Matcher for {@link $T#$L}\n", ownerType, property.getSimpleName())
                    .addStatement("return $L",
                            TypeSpec.anonymousClassBuilder(
                                    "$N, $S, $S",
                                    matcher,
                                    property.getSimpleName(),
                                    property.getSimpleName()
                            )
                                    .addSuperinterface(
                                            ParameterizedTypeName.get(
                                                    ClassName.get(FeatureMatcher.class),
                                                    ownerType,
                                                    propertyType
                                            )
                                    )
                                    .addMethod(
                                            MethodSpec.methodBuilder("featureValueOf")
                                                    .addAnnotation(Override.class)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(ownerType, "actual")
                                                    .returns(propertyType)
                                                    .addStatement(
                                                            isGetter ? "return $L.$L()" : "return $L.get$L()",
                                                            "actual",
                                                            isGetter ? property.getSimpleName() : Naming.normalize(property.getSimpleName())
                                                    )
                                                    .build()
                                    ).build()
                    )
                    .build();
        };
    }
}
