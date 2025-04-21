package io.github.itech_framework.java_fx.input.validations.validator;

@FunctionalInterface
public interface Validator<T> {
    ValidationResult validate(T value);
}
