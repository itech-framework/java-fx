package io.github.itech_framework.java_fx.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.itech_framework.core.annotations.components.Component;
import io.github.itech_framework.core.annotations.components.IgnoreInterfaces;
import io.github.itech_framework.core.annotations.components.policy.DisableLoaded;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@DisableLoaded
@IgnoreInterfaces
public @interface FxComponent {
    String value();
}
