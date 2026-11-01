package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public final class VirtualKeyboard extends Region {
    private final List<List<KeyboardKey>> rowKeys = new ArrayList<>();
    private final List<List<Node>> rowNodes = new ArrayList<>();
    private final List<String> cycle = new ArrayList<>();

    private KeyboardLayout layout;
    private TextInputControl target;
    private boolean shift;
    private double keyHeight = 52;
    private double gap = 6;
    private double maxKeyWidth = 96;
    private Runnable onHide = () -> {};
    
    private static final String LANGUAGE_LABEL = "\uD83C\uDF10";

    public VirtualKeyboard(String styleClass) {
        if (styleClass != null && !styleClass.isBlank()) {
            getStyleClass().add(styleClass);
        }
        setFocusTraversable(false);
    }

    public KeyboardLayout getLayout() {
        return layout;
    }

    public void setLayout(KeyboardLayout newLayout) {
        if (layout != null && layout.styleClass() != null) {
            getStyleClass().remove(layout.styleClass());
        }
        layout = newLayout;
        shift = false;
        if (layout != null && layout.styleClass() != null && !getStyleClass().contains(layout.styleClass())) {
            getStyleClass().add(layout.styleClass());
        }
        rebuild();
    }

    public void setTarget(TextInputControl input) {
        target = input;
        if (shift) {
            shift = false;
            rebuild();
        }
    }

    public TextInputControl getTarget() {
        return target;
    }

    public void setOnHide(Runnable action) {
        onHide = action == null ? () -> {} : action;
    }

    public void setCycle(List<String> layoutIds) {
        cycle.clear();
        if (layoutIds != null) {
            cycle.addAll(layoutIds);
        }
    }

    public void setKeyHeight(double value) {
        keyHeight = Math.max(24, value);
        requestLayout();
    }

    public void setGap(double value) {
        gap = Math.max(0, value);
        requestLayout();
    }

    public void setMaxKeyWidth(double value) {
        maxKeyWidth = Math.max(24, value);
        requestLayout();
    }

    public double naturalWidth() {
        return computePrefWidth(-1);
    }

    private void rebuild() {
        getChildren().clear();
        rowKeys.clear();
        rowNodes.clear();
        if (layout != null) {
        	for (List<KeyboardKey> row : effectiveRows()) {
                List<Node> nodes = new ArrayList<>();
                for (KeyboardKey key : row) {
                    nodes.add(createKey(key));
                }
                rowKeys.add(row);
                rowNodes.add(nodes);
                getChildren().addAll(nodes);
            }
        }
        requestLayout();
    }
    
    private List<List<KeyboardKey>> effectiveRows() {
        List<List<KeyboardKey>> rows = new ArrayList<>(layout.rows());
        if (!needsLanguageKey(rows)) {
            return rows;
        }
        int last = rows.size() - 1;
        List<KeyboardKey> bottom = new ArrayList<>(rows.get(last));
        int at = bottom.size();
        for (int i = 0; i < bottom.size(); i++) {
            if (bottom.get(i).action() == KeyAction.SPACE) {
                at = i;
                break;
            }
        }
        bottom.add(at, KeyboardKey.nextLayout(LANGUAGE_LABEL).styleClass("language-key"));
        rows.set(last, bottom);
        return rows;
    }

    private boolean needsLanguageKey(List<List<KeyboardKey>> rows) {
        if (rows.isEmpty() || cycle.size() < 2 || !cycle.contains(layout.id())) {
            return false;
        }
        for (List<KeyboardKey> row : rows) {
            for (KeyboardKey key : row) {
                if (key.action() == KeyAction.NEXT_LAYOUT) {
                    return false;
                }
            }
        }
        return true;
    }

    private Node createKey(KeyboardKey key) {
        Text text = new Text(shift ? key.shiftLabel() : key.label());
        text.getStyleClass().add("key-text");
        text.setMouseTransparent(true);
        StackPane node = new StackPane(text);
        node.getStyleClass().add("key");
        if (key.styleClass() != null && !key.styleClass().isBlank()) {
            node.getStyleClass().add(key.styleClass());
        }
        if (key.action() == KeyAction.SHIFT && shift) {
            node.getStyleClass().add("shift-active");
        }
        node.setFocusTraversable(false);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            event.consume();
            press(key);
        });
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, Event::consume);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, Event::consume);
        return node;
    }

    private void press(KeyboardKey key) {
        switch (key.action()) {
            case CHARACTER -> {
                insert(shift ? key.shiftValue() : key.value());
                if (shift) {
                    shift = false;
                    rebuild();
                }
            }
            case SPACE -> insert(key.value());
            case BACKSPACE -> backspace();
            case ENTER -> enter();
            case TAB -> tab();
            case CLEAR -> clear();
            case SHIFT -> {
                shift = !shift;
                rebuild();
            }
            case SWITCH_LAYOUT -> KeyboardLayouts.get(key.target()).ifPresent(this::setLayout);
            case NEXT_LAYOUT -> nextLayout();
            case HIDE -> onHide.run();
        }
    }

    private void insert(String value) {
        if (target != null && target.isEditable() && !value.isEmpty()) {
            target.replaceSelection(value);
        }
    }

    private void backspace() {
        if (target == null || !target.isEditable()) {
            return;
        }
        if (target.getSelection().getLength() > 0) {
            target.replaceSelection("");
        } else {
            target.deletePreviousChar();
        }
    }

    private void enter() {
        if (target == null) {
            return;
        }
        if (target instanceof TextArea) {
            insert("\n");
        } else {
            target.fireEvent(new ActionEvent(target, target));
        }
    }

    private void tab() {
        if (target == null) {
            return;
        }
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "\t", KeyCode.TAB,
                false, false, false, false));
    }

    private void clear() {
        if (target != null && target.isEditable()) {
            target.clear();
        }
    }

    private void nextLayout() {
        List<String> ids = cycle.isEmpty() ? KeyboardLayouts.ids() : cycle;
        if (ids.isEmpty() || layout == null) {
            return;
        }
        int index = ids.indexOf(layout.id());
        String nextId = ids.get((index + 1) % ids.size());
        KeyboardLayouts.get(nextId).ifPresent(this::setLayout);
    }

    private double unitFor(double available) {
        double unit = maxKeyWidth;
        for (List<KeyboardKey> row : rowKeys) {
            double weights = weightOf(row);
            double gaps = (row.size() - 1) * gap;
            unit = Math.min(unit, (available - gaps) / weights);
        }
        return Math.max(unit, 1);
    }

    private double weightOf(List<KeyboardKey> row) {
        double total = 0;
        for (KeyboardKey key : row) {
            total += key.weight();
        }
        return total;
    }

    @Override
    protected double computePrefWidth(double height) {
        double widest = 0;
        for (List<KeyboardKey> row : rowKeys) {
            widest = Math.max(widest, weightOf(row) * maxKeyWidth + (row.size() - 1) * gap);
        }
        return snappedLeftInset() + widest + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        int rows = rowKeys.size();
        double content = rows == 0 ? 0 : rows * keyHeight + (rows - 1) * gap;
        return snappedTopInset() + content + snappedBottomInset();
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double available = getWidth() - left - snappedRightInset();
        double unit = unitFor(available);
        double y = snappedTopInset();
        for (int r = 0; r < rowKeys.size(); r++) {
            List<KeyboardKey> keys = rowKeys.get(r);
            List<Node> nodes = rowNodes.get(r);
            double rowWidth = weightOf(keys) * unit + (keys.size() - 1) * gap;
            double x = left + Math.max(0, (available - rowWidth) / 2);
            for (int i = 0; i < keys.size(); i++) {
                double width = keys.get(i).weight() * unit;
                nodes.get(i).resizeRelocate(snapPositionX(x), snapPositionY(y), snapSizeX(width), keyHeight);
                x += width + gap;
            }
            y += keyHeight + gap;
        }
    }
}