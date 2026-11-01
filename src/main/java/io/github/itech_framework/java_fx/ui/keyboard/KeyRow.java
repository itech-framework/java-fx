package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class KeyRow {

    private KeyRow() {}

    public static List<KeyboardKey> of(Object... parts) {
        List<KeyboardKey> keys = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof KeyboardKey key) {
                keys.add(key);
            } else if (part instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof KeyboardKey key) {
                        keys.add(key);
                    } else {
                        throw new IllegalArgumentException("Row collections may only contain KeyboardKey");
                    }
                }
            } else {
                throw new IllegalArgumentException("Row parts must be KeyboardKey or a collection of KeyboardKey");
            }
        }
        return keys;
    }
}