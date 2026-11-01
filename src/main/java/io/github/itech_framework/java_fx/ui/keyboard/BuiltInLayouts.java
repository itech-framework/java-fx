package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.List;

final class BuiltInLayouts {

    private BuiltInLayouts() {}

    static List<KeyboardLayout> all() {
        return List.of(qwerty(), symbols(), numeric(), decimal());
    }

    static KeyboardLayout qwerty() {
        return KeyboardLayout.builder("qwerty")
                .name("English")
                .row(KeyboardKey.characters("1234567890").stream().map(k -> k.styleClass("number-key")).toList())
                .row(KeyboardKey.characters("qwertyuiop", "QWERTYUIOP"))
                .row(KeyboardKey.characters("asdfghjkl", "ASDFGHJKL"))
                .row(KeyRow.of(KeyboardKey.shift(), KeyboardKey.characters("zxcvbnm", "ZXCVBNM"), KeyboardKey.backspace()))
                .row(KeyRow.of(
                        KeyboardKey.switchTo("symbols", "?123"),
                        KeyboardKey.character(",").weight(1),
                        KeyboardKey.space().weight(5),
                        KeyboardKey.character("."),
                        KeyboardKey.enter(),
                        KeyboardKey.hide()))
                .build();
    }

    static KeyboardLayout symbols() {
        return KeyboardLayout.builder("symbols")
                .name("Symbols")
                .row(KeyboardKey.characters("1234567890").stream().map(k -> k.styleClass("number-key")).toList())
                .row(KeyboardKey.characters("[]{}#%^*+="))
                .row(KeyboardKey.characters("-/:;()$&@\""))
                .row(KeyRow.of(KeyboardKey.characters(".,?!'_~"), KeyboardKey.backspace()))
                .row(KeyRow.of(
                        KeyboardKey.switchTo(KeyboardLayouts.DEFAULT_ID, "ABC"),
                        KeyboardKey.space().weight(6),
                        KeyboardKey.enter(),
                        KeyboardKey.hide()))
                .build();
    }

    static KeyboardLayout numeric() {
        return KeyboardLayout.builder("numeric")
                .name("Numeric")
                .styleClass("keyboard-numeric")
                .row(KeyboardKey.characters("123"))
                .row(KeyboardKey.characters("456"))
                .row(KeyboardKey.characters("789"))
                .row(KeyRow.of(KeyboardKey.hide(), KeyboardKey.character("0"), KeyboardKey.backspace()))
                .row(KeyboardKey.enter().weight(1))
                .build();
    }

    static KeyboardLayout decimal() {
        return KeyboardLayout.builder("decimal")
                .name("Decimal")
                .styleClass("keyboard-numeric")
                .row(KeyboardKey.characters("123"))
                .row(KeyboardKey.characters("456"))
                .row(KeyboardKey.characters("789"))
                .row(KeyRow.of(KeyboardKey.character("."), KeyboardKey.character("0"), KeyboardKey.backspace()))
                .row(KeyRow.of(KeyboardKey.hide(), KeyboardKey.enter()))
                .build();
    }
}