package io.github.itech_framework.java_fx.ui.keyboard;

/**
 * Decides which text inputs get the on-screen keyboard when
 * {@code fx.keyboard.enabled=true}.
 * <p>
 * The scope is read from {@code fx.keyboard.scope} and can be changed in code
 * with {@link KeyboardConfig#scope(KeyboardScope)}.
 * </p>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * fx.keyboard.enabled=true
 * fx.keyboard.scope=ANNOTATED
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>A field with {@code @Keyboard(enabled = false)} never gets a keyboard in
 * either scope.</li>
 * <li>Read-only, disabled and invisible fields never get a keyboard.</li>
 * </ul>
 *
 * @author SaiZawMyint
 * @see KeyboardConfig
 * @see io.github.itech_framework.java_fx.annotations.Keyboard
 * @since 1.1.0
 */
public enum KeyboardScope {

	/**
	 * Every editable, enabled and visible text input gets the keyboard.
	 * {@code @Keyboard} only customizes a field or opts it out.
	 */
	ALL,

	/**
	 * Only fields annotated with {@code @Keyboard} get the keyboard.
	 */
	ANNOTATED
}