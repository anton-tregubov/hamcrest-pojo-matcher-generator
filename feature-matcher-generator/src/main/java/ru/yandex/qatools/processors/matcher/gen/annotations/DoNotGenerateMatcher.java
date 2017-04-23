package ru.yandex.qatools.processors.matcher.gen.annotations;

import java.lang.annotation.*;

/**
 * Don't generate matchers for annotated property/class
 *
 * @author lanwen (Merkushev Kirill)
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface DoNotGenerateMatcher {
}
