package ru.yandex.qatools.proc.test;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface ImPojo
{
    String getStringProperty();

    long getLongProperty();

    int getIntegerProperty();

    boolean isBooleanProperty();

    Optional<String> getOptioanlProperty();
}
