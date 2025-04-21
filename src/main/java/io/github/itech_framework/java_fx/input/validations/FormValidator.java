package io.github.itech_framework.java_fx.input.validations;

import javafx.scene.Node;

import java.util.*;
import java.util.function.Supplier;

import javafx.scene.control.*;
import io.github.itech_framework.java_fx.input.validations.validator.ValidationResult;
import io.github.itech_framework.java_fx.input.validations.validator.Validator;

public class FormValidator {
    private final Map<Node, List<ValidationRule<?>>> controlRules = new HashMap<>();
    private final List<CustomValidationRule> customRules = new ArrayList<>();
    private final ErrorDisplayStrategy errorDisplay = new DefaultErrorDisplay();

    public <T> FormValidator addRule(Node control, Validator<T> validator, String errorMessage) {
        controlRules.computeIfAbsent(control, k -> new ArrayList<>())
                .add(new ValidationRule<>(validator, errorMessage));
        return this;
    }

    public FormValidator addRequiredField(TextInputControl field) {
        return addRule(field, FormValidator.required(), "This field is required");
    }

    public FormValidator addEmailField(TextInputControl field) {
        return addRule(field, FormValidator.email(), "Invalid email address");
    }



    public FormValidator addCustomRule(Supplier<ValidationResult> validationCheck,
                                       Node... affectedControls) {
        customRules.add(new CustomValidationRule(validationCheck, affectedControls));
        return this;
    }

    public <T> FormValidator addComplexRule(Validator<T> validator,
                                            String errorMessage,
                                            Node... relatedControls) {
        customRules.add(new CustomValidationRule(
                () -> validator.validate(null),
                errorMessage,
                relatedControls
        ));
        return this;
    }

    public boolean validate() {
        boolean isValid = true;
        clearErrors();

        // Validate control-specific rules
        for (Map.Entry<Node, List<ValidationRule<?>>> entry : controlRules.entrySet()) {
            Node control = entry.getKey();
            for (ValidationRule<?> rule : entry.getValue()) {
                ValidationResult result = rule.validate(control);
                if (!result.isValid()) {
                    errorDisplay.showError(control, result.getMessage());
                    isValid = false;
                }
            }
        }

        // Validate custom rules
        for (CustomValidationRule rule : customRules) {
            ValidationResult result = rule.validate();
            if (!result.isValid()) {
                errorDisplay.showErrors(rule.getAffectedControls(), result.getMessage());
                isValid = false;
            }
        }

        return isValid;
    }

    public void clearErrors() {
        controlRules.keySet().forEach(errorDisplay::clearError);
        customRules.forEach(rule -> errorDisplay.clearErrors(rule.getAffectedControls()));
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
                value != null && value.doubleValue() >= min,
                String.format("Value must be at least %s", min)
        );
    }

    // Validation Result Class


    // Validation Rule Implementation
    private static class ValidationRule<T> {
        private final Validator<T> validator;
        private final String errorMessage;

        ValidationRule(Validator<T> validator, String errorMessage) {
            this.validator = validator;
            this.errorMessage = errorMessage;
        }

        @SuppressWarnings("unchecked")
        ValidationResult validate(Node node) {
            try {
                Object value = null;

                if (node instanceof TextInputControl textInput) {
                    value = textInput.getText();
                } else if (node instanceof ComboBox<?> comboBox) {
                    value = comboBox.getValue();
                } else if (node instanceof DatePicker datePicker) {
                    value = datePicker.getValue();
                }else if (node instanceof CheckBox checkBox) {
                    value = checkBox.isSelected();
                }

                if (value == null) {
                    return new ValidationResult(false, errorMessage);
                }

                ValidationResult result = validator.validate((T) value);
                return result.getMessage().isEmpty() ?
                        new ValidationResult(result.isValid(), errorMessage) : result;

            } catch (ClassCastException e) {
                return new ValidationResult(false,
                        "Type mismatch for control: " + node.getClass().getSimpleName()
                );
            }
        }
    }

    // Custom Validation Rule Implementation
    private static class CustomValidationRule {
        private final Supplier<ValidationResult> check;
        private final List<Node> affectedControls;
        private final String message;

        CustomValidationRule(Supplier<ValidationResult> check, Node... affectedControls) {
            this.check = check;
            this.affectedControls = Arrays.asList(affectedControls);
            this.message = "";
        }

        CustomValidationRule(Supplier<ValidationResult> check, String message, Node... affectedControls) {
            this.check = check;
            this.affectedControls = Arrays.asList(affectedControls);
            this.message = message;
        }

        ValidationResult validate() {
            ValidationResult result = check.get();
            return result.getMessage().isEmpty() ?
                    new ValidationResult(result.isValid(), message) : result;
        }

        List<Node> getAffectedControls() {
            return affectedControls;
        }
    }

    // Error Display Strategy
    public interface ErrorDisplayStrategy {
        void showError(Node node, String message);
        void showErrors(List<Node> nodes, String message);
        void clearError(Node node);
        void clearErrors(List<Node> nodes);
    }

    public static class DefaultErrorDisplay implements ErrorDisplayStrategy {
        @Override
        public void showError(Node node, String message) {
            applyErrorStyle(node, message);
        }

        @Override
        public void showErrors(List<Node> nodes, String message) {
            nodes.forEach(node -> applyErrorStyle(node, message));
        }

        @Override
        public void clearError(Node node) {
            removeErrorStyle(node);
        }

        @Override
        public void clearErrors(List<Node> nodes) {
            nodes.forEach(this::removeErrorStyle);
        }

        private void applyErrorStyle(Node node, String message) {
            node.getStyleClass().add("error-field");
            node.setStyle("-fx-border-color: #ff0000; -fx-border-width: 1.5px;");


            if(node instanceof Control control){
                if (control.getTooltip() == null || !control.tooltipProperty().isBound()) {
                    Tooltip tooltip = new Tooltip(message);
                    tooltip.setStyle("-fx-font-size: 12; -fx-text-fill: #ff0000;");
                    control.setTooltip(tooltip);
                }
            }

        }

        private void removeErrorStyle(Node node) {
            node.getStyleClass().remove("error-field");
            node.setStyle("");


            if(node instanceof  Control control){
                if (control.getTooltip() != null && !control.tooltipProperty().isBound()) {
                    control.setTooltip(null);
                }
            }

        }
    }

}