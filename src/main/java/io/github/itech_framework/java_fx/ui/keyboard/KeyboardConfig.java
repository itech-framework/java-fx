package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.scene.control.TextInputControl;

/**
 * Global keyboard settings that a {@link KeyboardConfigurer} can change in
 * code.
 * <p>
 * The object starts with the values read from the {@code fx.keyboard.*}
 * properties. Every setter returns the same object, so calls can be chained.
 * </p>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * config.register(MyanmarLayout.create())
 * 		.layout("qwerty")
 * 		.layouts(List.of("qwerty", "myanmar"))
 * 		.layoutFor(PasswordField.class, "numeric")
 * 		.anchorId("contentContainer")
 * 		.maxWidth(900)
 * 		.keyHeight(60);
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>Size setters clamp their value to the same minimum as the properties.</li>
 * <li>{@code @Keyboard} on a field overrides everything set here.</li>
 * <li>Layout ids set here must be registered before startup finishes.</li>
 * </ul>
 *
 * @author SaiZawMyint
 * @see KeyboardConfigurer
 * @see io.github.itech_framework.java_fx.annotations.Keyboard
 * @since 1.1.0
 */
public final class KeyboardConfig {
    private static volatile KeyboardConfig current;

    private final Map<Class<?>, String> typeLayouts = new LinkedHashMap<>();
    private String layout;
    private List<String> layouts;
    private String anchorId;
    private double minWidth;
    private double maxWidth;
    private double keyHeight;
    private double keyGap;
    private double maxKeyWidth;
    private KeyboardScope scope;

    private KeyboardConfig() {}

    static KeyboardConfig from(FxSettings.Keyboard settings) {
        KeyboardConfig config = new KeyboardConfig();
        config.layout = settings.layout();
        config.layouts = settings.layouts();
        config.anchorId = settings.anchorId();
        config.minWidth = settings.minWidth();
        config.maxWidth = settings.maxWidth();
        config.keyHeight = settings.keyHeight();
        config.keyGap = settings.keyGap();
        config.maxKeyWidth = settings.maxKeyWidth();
        config.scope = settings.scope();
        return config;
    }

    static void install(KeyboardConfig config) {
        current = config;
    }

    /**
     * Returns the active global settings. They are created from the properties
     * on first use.
     *
     * @return the active settings (never {@code null})
     */
    public static KeyboardConfig current() {
        KeyboardConfig config = current;
        if (config == null) {
            synchronized (KeyboardConfig.class) {
                config = current;
                if (config == null) {
                    config = from(FxSettings.get().keyboard());
                    current = config;
                }
            }
        }
        return config;
    }

    /**
     * Registers a layout. A layout with the same id as an existing one, built-in
     * or not, replaces it.
     *
     * @param keyboardLayout the layout to register
     * @return this config
     */
    public KeyboardConfig register(KeyboardLayout keyboardLayout) {
        KeyboardLayouts.register(keyboardLayout);
        return this;
    }

    /**
     * Sets the default layout id.
     *
     * @param id a registered layout id
     * @return this config
     */
    public KeyboardConfig layout(String id) {
        this.layout = id;
        return this;
    }

    /**
     * Sets the layout ids used by the "next layout" key.
     *
     * @param ids registered layout ids, empty for all registered layouts
     * @return this config
     */
    public KeyboardConfig layouts(List<String> ids) {
        this.layouts = List.copyOf(ids);
        return this;
    }

    /**
     * Sets the default layout for fields of a given type, for example a numeric
     * layout for every {@link javafx.scene.control.PasswordField}. The most
     * specific matching type wins.
     *
     * @param type the field type
     * @param id   a registered layout id
     * @return this config
     */
    public KeyboardConfig layoutFor(Class<? extends TextInputControl> type, String id) {
        typeLayouts.put(type, id);
        return this;
    }

    /**
     * Sets the fx:id of the default anchor node.
     *
     * @param id the anchor id, empty to follow the window
     * @return this config
     */
    public KeyboardConfig anchorId(String id) {
        this.anchorId = id;
        return this;
    }

    /**
     * Sets the smallest keyboard width.
     *
     * @param value width in pixels, {@code 0} for no limit
     * @return this config
     */
    public KeyboardConfig minWidth(double value) {
        this.minWidth = Math.max(0, value);
        return this;
    }

    /**
     * Sets the largest keyboard width.
     *
     * @param value width in pixels, {@code 0} for no limit
     * @return this config
     */
    public KeyboardConfig maxWidth(double value) {
        this.maxWidth = Math.max(0, value);
        return this;
    }

    /**
     * Sets the key height.
     *
     * @param value height in pixels (at least 24)
     * @return this config
     */
    public KeyboardConfig keyHeight(double value) {
        this.keyHeight = Math.max(24, value);
        return this;
    }

    /**
     * Sets the gap between keys.
     *
     * @param value gap in pixels (at least 0)
     * @return this config
     */
    public KeyboardConfig keyGap(double value) {
        this.keyGap = Math.max(0, value);
        return this;
    }

    /**
     * Sets the widest a single key unit may grow.
     *
     * @param value width in pixels (at least 24)
     * @return this config
     */
    public KeyboardConfig maxKeyWidth(double value) {
        this.maxKeyWidth = Math.max(24, value);
        return this;
    }

    /**
     * Sets which fields get a keyboard.
     *
     * @param value the scope, {@code null} means {@link KeyboardScope#ALL}
     * @return this config
     */
    public KeyboardConfig scope(KeyboardScope value) {
        this.scope = value == null ? KeyboardScope.ALL : value;
        return this;
    }

    /** @return the default layout id */
    public String layout() { return layout; }

    /** @return the layout ids used by the "next layout" key */
    public List<String> layouts() { return layouts; }

    /** @return the default anchor id, empty when the window is followed */
    public String anchorId() { return anchorId; }

    /** @return the smallest keyboard width, {@code 0} for no limit */
    public double minWidth() { return minWidth; }

    /** @return the largest keyboard width, {@code 0} for no limit */
    public double maxWidth() { return maxWidth; }

    /** @return the key height in pixels */
    public double keyHeight() { return keyHeight; }

    /** @return the gap between keys in pixels */
    public double keyGap() { return keyGap; }

    /** @return the widest a single key unit may grow, in pixels */
    public double maxKeyWidth() { return maxKeyWidth; }

    /** @return which fields get a keyboard */
    public KeyboardScope scope() { return scope; }

    /**
     * Returns the layout id registered for the type of a field.
     *
     * @param input the field
     * @return the layout id of the most specific matching type, or {@code null}
     */
    public String layoutFor(TextInputControl input) {
        String best = null;
        Class<?> bestType = null;
        for (Map.Entry<Class<?>, String> entry : typeLayouts.entrySet()) {
            Class<?> type = entry.getKey();
            if (type.isInstance(input) && (bestType == null || bestType.isAssignableFrom(type))) {
                best = entry.getValue();
                bestType = type;
            }
        }
        return best;
    }

    List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (minWidth > 0 && maxWidth > 0 && minWidth > maxWidth) {
            problems.add("min-width (" + minWidth + ") is greater than max-width (" + maxWidth + ")");
        }
        requireLayout("fx.keyboard.layout", layout, problems);
        for (String id : layouts) {
            requireLayout("fx.keyboard.layouts", id, problems);
        }
        for (Map.Entry<Class<?>, String> entry : typeLayouts.entrySet()) {
            requireLayout("layoutFor(" + entry.getKey().getSimpleName() + ")", entry.getValue(), problems);
        }
        return problems;
    }

    private static void requireLayout(String source, String id, List<String> problems) {
        if (id == null || id.isBlank()) {
            problems.add(source + " is blank");
        } else if (KeyboardLayouts.get(id).isEmpty()) {
            problems.add(source + " refers to unknown layout '" + id + "'. Registered layouts: " + KeyboardLayouts.ids());
        }
    }
}