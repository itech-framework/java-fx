package io.github.itech_framework.java_fx.ui.keyboard;

import javafx.scene.control.TextInputControl;

/**
 * Builds a keyboard layout in code for one annotated field.
 * <p>
 * Use it with {@code @Keyboard(supplier = ...)} when the layout needs logic
 * that the inline {@code rows} syntax cannot express, for example keys that
 * depend on the field.
 * </p>
 *
 * <h2>How to implement</h2>
 * <ol>
 * <li>Create a class with a no-argument constructor that implements this
 * interface.</li>
 * <li>Build and return a {@link KeyboardLayout} from
 * {@link #create(TextInputControl)}.</li>
 * <li>Reference the class in {@code @Keyboard(supplier = MyLayout.class)}.</li>
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * public class PinLayout implements KeyboardLayoutSupplier {
 *
 * 	&#64;Override
 * 	public KeyboardLayout create(TextInputControl input) {
 * 		return KeyboardLayout.builder("pin-" + input.getId())
 * 				.name("PIN")
 * 				.row(List.of(KeyboardKey.character("1"), KeyboardKey.character("2"), KeyboardKey.character("3")))
 * 				.row(List.of(KeyboardKey.clear(), KeyboardKey.character("0"), KeyboardKey.backspace()))
 * 				.build();
 * 	}
 * }
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>The supplier is created with its no-argument constructor. It is not
 * injected by the DI container.</li>
 * <li>The returned layout is registered under its id. Layouts with the same id
 * replace each other, so use a different id for each field that needs a
 * different layout.</li>
 * <li>Returning {@code null} fails when the FXML is loaded.</li>
 * </ul>
 *
 * @author SaiZawMyint
 * @see io.github.itech_framework.java_fx.annotations.Keyboard
 * @see KeyboardLayout
 * @since 1.1.0
 */
@FunctionalInterface
public interface KeyboardLayoutSupplier {

	/**
	 * Creates the layout for a field.
	 *
	 * @param input the annotated field (never {@code null})
	 * @return the layout for this field (must not be {@code null})
	 */
	KeyboardLayout create(TextInputControl input);
}