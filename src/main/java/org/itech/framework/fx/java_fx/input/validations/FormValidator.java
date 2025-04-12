package org.itech.framework.fx.java_fx.input.validations;

import javafx.scene.Node;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.control.*;
import org.itech.framework.fx.java_fx.input.validations.validator.ValidationResult;
import org.itech.framework.fx.java_fx.input.validations.validator.Validator;

public class FormValidator {
    private final Map<Node, List<ValidationRule<?>>> rules = new HashMap<>();
    private final ErrorDisplayStrategy errorDisplay = new DefaultErrorDisplay();

    public <T> FormValidator addRule(Node control, Validator<T> validator, String errorMessage) {
        rules.computeIfAbsent(control, k -> new ArrayList<>())
                .add(new ValidationRule<>(validator, errorMessage));
        return this;
    }

    public boolean validate() {
        boolean isValid = true;
        clearErrors();

        for (Map.Entry<Node, List<ValidationRule<?>>> entry : rules.entrySet()) {
            Node control = entry.getKey();
            for (ValidationRule<?> rule : entry.getValue()) {
                ValidationResult result = rule.validate(control);
                if (!result.isValid()) {
                    errorDisplay.showError(control, result.getMessage());
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    public void clearErrors() {
        rules.keySet().forEach(errorDisplay::clearError);
    }

    // Built-in Validators
    public static Validator<String> required() {
        return value -> new ValidationResult(
                value != null && !value.trim().isEmpty(),
                "This field is required"
        );
    }

    public static Validator<String> email() {
        return value -> new ValidationResult(
                value.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"),
                "Invalid email format"
        );
    }

    public static Validator<Number> minValue(double min) {
        return value -> new ValidationResult(
                value.doubleValue() >= min,
                String.format("Value must be at least %s", min)
        );
    }

    // Fluent API
    public FormValidator addRequiredField(TextInputControl field) {
        return addRule(field, FormValidator.required(), "This field is required");
    }

    public FormValidator addEmailField(TextInputControl field) {
        return addRule(field, FormValidator.email(), "Invalid email address");
    }

    public interface ErrorDisplayStrategy {
        void showError(Node node, String message);
        void clearError(Node node);
    }

    public static class DefaultErrorDisplay implements ErrorDisplayStrategy {
        @Override
        public void showError(Node node, String message) {
            node.getStyleClass().add("error-field");

            Tooltip tooltip = new Tooltip(message);

            if (node instanceof TextInputControl) {
                TextInputControl input = (TextInputControl) node;
                if (!input.tooltipProperty().isBound()) {
                    input.setTooltip(tooltip);
                }
            } else if (node instanceof ComboBox<?> comboBox) {
                if (!comboBox.tooltipProperty().isBound()) {
                    comboBox.setTooltip(tooltip);
                }
            } else if (node instanceof DatePicker datePicker) {
                if (!datePicker.tooltipProperty().isBound()) {
                    datePicker.setTooltip(tooltip);
                }
            }
        }

        @Override
        public void clearError(Node node) {
            node.getStyleClass().remove("error-field");

            if (node instanceof TextInputControl) {
                TextInputControl input = (TextInputControl) node;
                if (!input.tooltipProperty().isBound()) {
                    input.setTooltip(null);
                }
            } else if (node instanceof ComboBox<?>) {
                ComboBox<?> comboBox = (ComboBox<?>) node;
                if (!comboBox.tooltipProperty().isBound()) {
                    comboBox.setTooltip(null);
                }
            } else if (node instanceof DatePicker) {
                DatePicker datePicker = (DatePicker) node;
                if (!datePicker.tooltipProperty().isBound()) {
                    datePicker.setTooltip(null);
                }
            }
        }
    }

    private static class ValidationRule<T> {
        private final Validator<T> validator;
        private final String errorMessage;

        ValidationRule(Validator<T> validator, String errorMessage) {
            this.validator = validator;
            this.errorMessage = errorMessage;
        }

        @SuppressWarnings("unchecked")
        ValidationResult validate(Node node) {
            if (node instanceof TextInputControl textInput) {
                String value = textInput.getText();
                if (value == null || value.trim().isEmpty()) {
                    return new ValidationResult(false, errorMessage);
                }
                return validator.validate((T) value);

            } else if (node instanceof ComboBox<?> comboBox) {
                Object value = comboBox.getValue();
                if (value == null) {
                    return new ValidationResult(false, errorMessage);
                }
                return validator.validate((T) value);

            } else if (node instanceof DatePicker datePicker) {
                LocalDate date = datePicker.getValue();
                if (date == null) {
                    return new ValidationResult(false, errorMessage);
                }
                return validator.validate((T) date.toString());

            } else if (node instanceof CheckBox checkBox) {
                // Example for additional control type
                return validator.validate((T) Boolean.valueOf(checkBox.isSelected()));

            } else {
                return new ValidationResult(false,
                        String.format("Unsupported control type: %s", node.getClass().getSimpleName())
                );
            }
        }
    }
}
