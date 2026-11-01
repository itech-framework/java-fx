package io.github.itech_framework.java_fx.ui.keyboard;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.stage.Window;

/**
 * Chooses the node whose width the on-screen keyboard follows.
 * <p>
 * The keyboard has no fixed width. It uses the width of an anchor node, for
 * example the form container or the content area. When no usable anchor exists
 * it uses the window.
 * </p>
 *
 * <h2>How the anchor is found</h2>
 * <ol>
 * <li>Starting at the focused field and walking up through its parents, the
 * first node that has an anchor assigned wins. One assignment on a form
 * container covers every field inside it.</li>
 * <li>The node with the id from {@code fx.keyboard.anchor-id} (or
 * {@code @Keyboard(anchor = ...)}).</li>
 * <li>The window.</li>
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * KeyboardAnchors.assign(formPane, formPane);
 * KeyboardAnchors.assign(searchField, "searchBar");
 * KeyboardAnchors.clear(searchField);
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>An anchor can be a {@link Node} or the fx:id of a node, which is looked
 * up in the scene of the focused field.</li>
 * <li>An anchor that is not showing, or has zero width, is ignored and the
 * window is used.</li>
 * </ul>
 *
 * @author SaiZawMyint
 * @see KeyboardConfig#anchorId(String)
 * @see io.github.itech_framework.java_fx.annotations.ui.Keyboard#anchor()
 * @since 1.1.0
 */
public final class KeyboardAnchors {
    public static final String PROPERTY = "fx.keyboard.anchor";

    private KeyboardAnchors() {}

    /**
     * Assigns an anchor node to a node. The assignment applies to the node and
     * to every field below it.
     *
     * @param node   the node that receives the assignment
     * @param anchor the node whose width the keyboard follows
     */
    public static void assign(Node node, Node anchor) {
        store(node, anchor);
    }

    /**
     * Assigns an anchor by fx:id to a node. The id is looked up in the scene of
     * the focused field.
     *
     * @param node     the node that receives the assignment
     * @param anchorId the fx:id of the anchor
     */
    public static void assign(Node node, String anchorId) {
        store(node, anchorId);
    }

    /**
     * Removes the anchor assignment of a node.
     *
     * @param node the node to clear
     */
    public static void clear(Node node) {
        store(node, null);
    }

    /**
     * Returns the screen bounds the keyboard follows.
     *
     * @param input        the focused field
     * @param configuredId the default anchor id, may be empty
     * @param window       the window used when no anchor is usable
     * @return the anchor bounds, or the window bounds
     */
    public static Bounds resolve(TextInputControl input, String configuredId, Window window) {
        Bounds bounds = anchorBounds(input, configuredId);
        if (bounds != null) {
            return bounds;
        }
        return new BoundingBox(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    }

    /**
     * Returns the screen bounds of the anchor, without the window fallback.
     *
     * @param input        the focused field
     * @param configuredId the default anchor id, may be empty
     * @return the anchor bounds, or {@code null} when no usable anchor exists
     */
    public static Bounds anchorBounds(TextInputControl input, String configuredId) {
        Node anchor = find(input, configuredId);
        if (anchor == null) {
            return null;
        }
        Bounds bounds = screenBounds(anchor);
        return bounds != null && bounds.getWidth() > 0 ? bounds : null;
    }

    /**
     * Finds the anchor node of a field.
     *
     * @param input        the focused field
     * @param configuredId the default anchor id, may be empty
     * @return the anchor node, or {@code null} when none is assigned
     */
    public static Node find(TextInputControl input, String configuredId) {
        Scene scene = input.getScene();
        for (Node node = input; node != null; node = node.getParent()) {
            Node anchor = toNode(node.getProperties().get(PROPERTY), scene);
            if (anchor != null) {
                return anchor;
            }
        }
        return toNode(configuredId, scene);
    }

    private static void store(Node node, Object value) {
        if (value == null) {
            node.getProperties().remove(PROPERTY);
        } else {
            node.getProperties().put(PROPERTY, value);
        }
    }

    private static Node toNode(Object value, Scene scene) {
        if (value instanceof Node node) {
            return node;
        }
        if (value instanceof String id && !id.isBlank() && scene != null) {
            return scene.getRoot().lookup("#" + id.trim());
        }
        return null;
    }

    private static Bounds screenBounds(Node node) {
        Scene scene = node.getScene();
        if (scene == null || scene.getWindow() == null || !scene.getWindow().isShowing()) {
            return null;
        }
        return node.localToScreen(node.getLayoutBounds());
    }
}