package io.github.itech_framework.java_fx.utils.node;

import javafx.scene.Node;
import java.util.*;
import java.util.stream.Collectors;

public final class StyleUtils {

    private StyleUtils() {}

    /**
     * Applies styles while preserving existing properties
     * @param node Target node to modify
     * @param styles CSS properties in format "-fx-property: value;"
     */
    public static void addStyles(Node node, String styles) {
        String existing = node.getStyle();
        String merged = mergeStyles(existing, styles);
        node.setStyle(merged);
    }

    /**
     * Removes specific style properties from a node
     * @param node Target node
     * @param properties List of properties to remove (e.g., "-fx-font-family")
     */
    public static void removeStyles(Node node, String... properties) {
        Map<String, String> styleMap = parseStyleToMap(node.getStyle());
        for (String prop : properties) {
            styleMap.remove(prop);
        }
        node.setStyle(joinStyles(styleMap));
    }

    /**
     * Sets font family while preserving other styles
     * @param node Target node
     * @param fontFamilies Ordered list of font families
     */
    public static void setFontFamily(Node node, List<String> fontFamilies) {
        String fontValue = fontFamilies.stream()
                .map(StyleUtils::formatFontName)
                .collect(Collectors.joining(", ")) + ", sans-serif";

        addStyles(node, "-fx-font-family: " + fontValue);
    }

    /**
     * Merges multiple style strings
     */
    public static String mergeStyles(String... styles) {
        return Arrays.stream(styles)
                .map(StyleUtils::parseStyleToMap)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> newVal))
                .entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
    }

    private static Map<String, String> parseStyleToMap(String style) {
        return Arrays.stream(style.split(";"))
                .map(String::trim)
                .filter(prop -> !prop.isEmpty())
                .map(prop -> {
                    int colonIndex = prop.indexOf(':');
                    return colonIndex > 0
                            ? new AbstractMap.SimpleEntry<>(
                            prop.substring(0, colonIndex).trim(),
                            prop.substring(colonIndex + 1).trim())
                            : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> newVal));
    }

    private static String joinStyles(Map<String, String> styleMap) {
        return styleMap.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
    }

    private static String formatFontName(String fontName) {
        return fontName.contains(" ") ? "'" + fontName + "'" : fontName;
    }

    /**
     * Checks if a node has a specific style property
     */
    public static boolean hasStyleProperty(Node node, String property) {
        return parseStyleToMap(node.getStyle()).containsKey(property);
    }

    /**
     * Gets the value of a specific style property
     */
    public static Optional<String> getStyleValue(Node node, String property) {
        return Optional.ofNullable(
                parseStyleToMap(node.getStyle()).get(property)
        );
    }
}