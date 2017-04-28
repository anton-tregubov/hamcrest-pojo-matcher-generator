package ru.yandex.qatools.proc.test;

import org.immutables.value.Value;
import ru.yandex.qatools.processors.matcher.gen.annotations.GenerateMatcher;

import java.util.Optional;

@Value.Immutable
@GenerateMatcher
public interface ImPojo
{
    String getStringProperty();

    long getLongProperty();

    int getIntegerProperty();

    boolean isBooleanProperty();

    Optional<String> getOptionalProperty();
}
