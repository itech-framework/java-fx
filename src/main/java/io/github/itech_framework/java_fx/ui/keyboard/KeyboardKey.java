package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record KeyboardKey(KeyAction action, String label, String shiftLabel, String value, String shiftValue,
                          String target, double weight, String styleClass) {

    public KeyboardKey {
        Objects.requireNonNull(action, "action must not be null");
        label = label == null ? "" : label;
        shiftLabel = shiftLabel == null ? label : shiftLabel;
        value = value == null ? "" : value;
        shiftValue = shiftValue == null ? value : shiftValue;
        weight = weight <= 0 ? 1 : weight;
    }

    public static KeyboardKey character(String value) {
        return new KeyboardKey(KeyAction.CHARACTER, value, value, value, value, null, 1, null);
    }

    public static KeyboardKey character(String value, String shiftValue) {
        return new KeyboardKey(KeyAction.CHARACTER, value, shiftValue, value, shiftValue, null, 1, null);
    }

    public static List<KeyboardKey> characters(String values) {
        List<KeyboardKey> keys = new ArrayList<>();
        values.codePoints().forEach(cp -> keys.add(character(new String(Character.toChars(cp)))));
        return keys;
    }

    public static List<KeyboardKey> characters(String values, String shiftValues) {
        int[] lower = values.codePoints().toArray();
        int[] upper = shiftValues.codePoints().toArray();
        if (lower.length != upper.length) {
            throw new IllegalArgumentException("Normal and shift characters must have the same length");
        }
        List<KeyboardKey> keys = new ArrayList<>();
        for (int i = 0; i < lower.length; i++) {
            keys.add(character(new String(Character.toChars(lower[i])), new String(Character.toChars(upper[i]))));
        }
        return keys;
    }

    public static List<KeyboardKey> characters(String[] values) {
        List<KeyboardKey> keys = new ArrayList<>();
        for (String value : values) {
            keys.add(character(value));
        }
        return keys;
    }

    public static KeyboardKey space() {
        return space("");
    }

    public static KeyboardKey space(String label) {
        return new KeyboardKey(KeyAction.SPACE, label, label, " ", " ", null, 1, "space-key");
    }

    public static KeyboardKey backspace() {
        return backspace("Back");
    }

    public static KeyboardKey backspace(String label) {
        return special(KeyAction.BACKSPACE, label, null);
    }

    public static KeyboardKey enter() {
        return enter("Enter");
    }

    public static KeyboardKey enter(String label) {
        return special(KeyAction.ENTER, label, null);
    }

    public static KeyboardKey tab() {
        return special(KeyAction.TAB, "Tab", null);
    }

    public static KeyboardKey clear() {
        return special(KeyAction.CLEAR, "Clear", null);
    }

    public static KeyboardKey shift() {
        return shift("Shift");
    }

    public static KeyboardKey shift(String label) {
        return special(KeyAction.SHIFT, label, null);
    }

    public static KeyboardKey hide() {
        return hide("Hide");
    }

    public static KeyboardKey hide(String label) {
        return special(KeyAction.HIDE, label, null);
    }

    public static KeyboardKey switchTo(String layoutId, String label) {
        return special(KeyAction.SWITCH_LAYOUT, label, Objects.requireNonNull(layoutId, "layoutId must not be null"));
    }

    public static KeyboardKey nextLayout(String label) {
        return special(KeyAction.NEXT_LAYOUT, label, null);
    }

    public KeyboardKey weight(double newWeight) {
        return new KeyboardKey(action, label, shiftLabel, value, shiftValue, target, newWeight, styleClass);
    }

    public KeyboardKey styleClass(String newStyleClass) {
        return new KeyboardKey(action, label, shiftLabel, value, shiftValue, target, weight, newStyleClass);
    }

    public KeyboardKey label(String newLabel) {
        return new KeyboardKey(action, newLabel, newLabel, value, shiftValue, target, weight, styleClass);
    }

    public boolean isSpecial() {
        return action != KeyAction.CHARACTER;
    }

    private static KeyboardKey special(KeyAction action, String label, String target) {
        return new KeyboardKey(action, label, label, "", "", target, 1.5, "special-key");
    }
}