package org.itech.framework.fx.java_fx.input.validations.validator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResult {
    private final boolean valid;
    private final String message;

    public ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
}