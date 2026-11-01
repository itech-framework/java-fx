package io.github.itech_framework.java_fx.annotations.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.itech_framework.java_fx.ui.keyboard.KeyboardLayoutSupplier;

/**
 * Customizes the on-screen keyboard of one text input field, or opts the field
 * out of the keyboard.
 * <p>
 * The annotation is placed on a controller field that is injected from FXML.
 * It is read when the FXML is loaded and the result is stored on the control,
 * so the keyboard popup can use it when the field gets focus. It only has an
 * effect when {@code fx.keyboard.enabled=true}.
 * </p>
 *
 * <h2>Why use this annotation?</h2>
 * <p>
 * The global properties give every field the same keyboard. Some fields need
 * something else: a number pad for a PIN, a decimal pad for an amount, another
 * language for a name, or no keyboard at all for a barcode scanner input. This
 * annotation changes only that field. Every attribute left at its default
 * keeps the global value.
 * </p>
 *
 * <h2>How to use</h2>
 * <ol>
 * <li>Add {@code fx:id} to the control in the FXML file and a matching
 * {@code @FXML} field in the controller.</li>
 * <li>Add {@code @Keyboard} to the field.</li>
 * <li>Choose the layout with <b>one</b> of {@link #layout()}, {@link #rows()}
 * or {@link #supplier()}.</li>
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * &#64;FxController
 * public class PaymentController {
 *
 * 	&#64;FXML
 * 	&#64;Keyboard(rows = {"1 2 3", "4 5 6", "7 8 9", "{clear} 0 {back}", "{enter=OK*2} {hide}"})
 * 	private PasswordField pin;
 *
 * 	&#64;FXML
 * 	&#64;Keyboard(layout = "decimal", anchor = "formPane", maxWidth = 360)
 * 	private TextField amount;
 *
 * 	&#64;FXML
 * 	&#64;Keyboard(layouts = {"qwerty", "myanmar"}, keyHeight = 60)
 * 	private TextField customerName;
 *
 * 	&#64;FXML
 * 	&#64;Keyboard(enabled = false)
 * 	private TextField barcode;
 * }
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>Only {@link javafx.scene.control.TextInputControl} fields are supported
 * (TextField, PasswordField, TextArea).</li>
 * <li>Use only one of {@link #layout()}, {@link #rows()} and
 * {@link #supplier()} on a field. Startup fails otherwise.</li>
 * <li>Numeric attributes use {@code -1} for "not set", which keeps the global
 * value.</li>
 * <li>With {@code fx.keyboard.scope=ANNOTATED}, only annotated fields get a
 * keyboard.</li>
 * </ul>
 *
 * @author SaiZawMyint
 * @see io.github.itech_framework.java_fx.ui.keyboard.KeyboardConfigurer
 * @see io.github.itech_framework.java_fx.ui.keyboard.KeyboardLayoutSupplier
 * @see io.github.itech_framework.java_fx.ui.keyboard.KeyboardScope
 * @since 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Keyboard {

	/**
	 * Whether this field gets a keyboard. Set to {@code false} to opt the field
	 * out, for example for a barcode scanner input.
	 *
	 * @return {@code true} to show the keyboard for this field
	 */
	boolean enabled() default true;

	/**
	 * Id of a registered layout, for example {@code numeric} or {@code decimal}.
	 * An unknown id fails when the FXML is loaded.
	 *
	 * @return the layout id, empty to keep the default
	 */
	String layout() default "";

	/**
	 * Layout ids used by the "next layout" key on this field.
	 *
	 * @return the layout ids, empty to keep the global list
	 */
	String[] layouts() default {};

	/**
	 * Inline layout, one string per row. Keys are separated by spaces, special
	 * keys are written in braces such as {@code {back}} and {@code {space*5}}.
	 *
	 * @return the rows, empty when the layout comes from another attribute
	 */
	String[] rows() default {};

	/**
	 * Class that builds the layout in code. It needs a public or non-public
	 * no-argument constructor and is not created by dependency injection.
	 *
	 * @return the supplier class, {@link KeyboardLayoutSupplier} itself means
	 *         "not set"
	 */
	Class<? extends KeyboardLayoutSupplier> supplier() default KeyboardLayoutSupplier.class;

	/**
	 * Id of the node whose width the keyboard follows for this field.
	 *
	 * @return the fx:id of the anchor, empty to keep the global anchor
	 */
	String anchor() default "";

	/**
	 * Key height in pixels.
	 *
	 * @return the key height, {@code -1} to keep the global value
	 */
	double keyHeight() default -1;

	/**
	 * Gap between keys in pixels.
	 *
	 * @return the key gap, {@code -1} to keep the global value
	 */
	double keyGap() default -1;

	/**
	 * Widest a single key unit may grow, in pixels.
	 *
	 * @return the maximum key width, {@code -1} to keep the global value
	 */
	double maxKeyWidth() default -1;

	/**
	 * Smallest keyboard width in pixels.
	 *
	 * @return the minimum width, {@code -1} to keep the global value
	 */
	double minWidth() default -1;

	/**
	 * Largest keyboard width in pixels.
	 *
	 * @return the maximum width, {@code -1} to keep the global value
	 */
	double maxWidth() default -1;
}