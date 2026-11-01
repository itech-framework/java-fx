package io.github.itech_framework.java_fx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.itech_framework.java_fx.annotations.components.FxScope;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FxUse {

    FxScope scope() default FxScope.OWNER;
}