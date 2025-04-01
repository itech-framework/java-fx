package org.itech.framework.fx.java_fx.annotations;

import org.itech.framework.fx.core.annotations.components.Component;
import org.itech.framework.fx.core.annotations.components.IgnoreInterfaces;
import org.itech.framework.fx.core.annotations.components.levels.Presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@IgnoreInterfaces
public @interface FxController {
}
