package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class KeyboardRows {

    private KeyboardRows() {}

    static KeyboardLayout parse(String id, String[] rows) {
        KeyboardLayout.Builder builder = KeyboardLayout.builder(id).name(id);
        for (int r = 0; r < rows.length; r++) {
            List<KeyboardKey> keys = new ArrayList<>();
            for (String token : rows[r].trim().split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                keys.add(token.startsWith("{") ? special(token, r) : character(token));
            }
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("row " + (r + 1) + " has no keys");
            }
            builder.row(keys);
        }
        return builder.build();
    }

    private static KeyboardKey character(String token) {
        int slash = token.indexOf('/', 1);
        if (slash > 0 && slash < token.length() - 1) {
            return KeyboardKey.character(token.substring(0, slash), token.substring(slash + 1));
        }
        return KeyboardKey.character(token);
    }

    private static KeyboardKey special(String token, int row) {
        if (!token.endsWith("}")) {
            throw new IllegalArgumentException("row " + (row + 1) + ": unclosed key " + token);
        }
        String body = token.substring(1, token.length() - 1);
        double weight = -1;
        int star = body.lastIndexOf('*');
        if (star >= 0) {
            try {
                weight = Double.parseDouble(body.substring(star + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("row " + (row + 1) + ": bad weight in " + token);
            }
            body = body.substring(0, star);
        }
        String label = null;
        int equals = body.indexOf('=');
        if (equals >= 0) {
            label = body.substring(equals + 1);
            body = body.substring(0, equals);
        }
        String arg = null;
        int colon = body.indexOf(':');
        if (colon >= 0) {
            arg = body.substring(colon + 1);
            body = body.substring(0, colon);
        }
        KeyboardKey key = switch (body.toLowerCase(Locale.ROOT)) {
            case "shift" -> KeyboardKey.shift();
            case "back" -> KeyboardKey.backspace();
            case "enter" -> KeyboardKey.enter();
            case "space" -> KeyboardKey.space();
            case "tab" -> KeyboardKey.tab();
            case "clear" -> KeyboardKey.clear();
            case "hide" -> KeyboardKey.hide();
            case "next" -> KeyboardKey.nextLayout("Next");
            case "layout" -> {
                if (arg == null || arg.isBlank()) {
                    throw new IllegalArgumentException("row " + (row + 1) + ": " + token + " needs a layout id, e.g. {layout:symbols}");
                }
                yield KeyboardKey.switchTo(arg, arg);
            }
            default -> throw new IllegalArgumentException("row " + (row + 1) + ": unknown key " + token);
        };
        if (label != null) {
            key = key.label(label);
        }
        if (weight > 0) {
            key = key.weight(weight);
        }
        return key;
    }
}