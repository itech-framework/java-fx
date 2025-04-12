package org.itech.framework.fx.java_fx.input.validations.validator;

public interface Validator<T> {
    ValidationResult validate(T value);
}
