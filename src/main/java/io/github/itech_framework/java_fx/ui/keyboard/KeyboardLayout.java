package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class KeyboardLayout {
    private final String id;
    private final String name;
    private final String styleClass;
    private final List<List<KeyboardKey>> rows;
    private final boolean shiftable;

    private KeyboardLayout(String id, String name, String styleClass, List<List<KeyboardKey>> rows) {
        this.id = id;
        this.name = name;
        this.styleClass = styleClass;
        List<List<KeyboardKey>> copy = new ArrayList<>();
        boolean shift = false;
        for (List<KeyboardKey> row : rows) {
            copy.add(List.copyOf(row));
            for (KeyboardKey key : row) {
                if (key.action() == KeyAction.SHIFT) {
                    shift = true;
                }
            }
        }
        this.rows = Collections.unmodifiableList(copy);
        this.shiftable = shift;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String styleClass() { return styleClass; }
    public List<List<KeyboardKey>> rows() { return rows; }
    public boolean shiftable() { return shiftable; }

    public static final class Builder {
        private final String id;
        private String name;
        private String styleClass;
        private final List<List<KeyboardKey>> rows = new ArrayList<>();

        private Builder(String id) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Layout id must not be blank");
            }
            this.id = id;
            this.name = id;
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        public Builder styleClass(String styleClass) {
            this.styleClass = styleClass;
            return this;
        }

        public Builder row(KeyboardKey... keys) {
            return row(List.of(keys));
        }

        public Builder row(List<KeyboardKey> keys) {
            if (keys.isEmpty()) {
                throw new IllegalArgumentException("A keyboard row must have at least one key");
            }
            rows.add(new ArrayList<>(keys));
            return this;
        }

        public KeyboardLayout build() {
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Layout '" + id + "' must have at least one row");
            }
            for (List<KeyboardKey> row : rows) {
                for (KeyboardKey key : row) {
                    if (key.action() == KeyAction.SWITCH_LAYOUT && (key.target() == null || key.target().isBlank())) {
                        throw new IllegalArgumentException("Layout '" + id + "' has a switch key without a target");
                    }
                }
            }
            return new KeyboardLayout(id, name, styleClass, rows);
        }
    }
}