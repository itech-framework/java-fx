package io.github.itech_framework.java_fx.ui.keyboard;

/**
 * Callback that changes the global keyboard settings in code, after the
 * properties are read and before any field is bound.
 * <p>
 * Use it for things properties cannot express: registering extra layouts,
 * choosing a layout by field type, or overriding a built-in layout.
 * </p>
 *
 * <h2>How to implement</h2>
 * <ol>
 * <li>Create a class that implements this interface and annotate it with
 * {@code @Component}, so the core framework injects its fields.</li>
 * <li>Change the settings on the given {@link KeyboardConfig}.</li>
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * &#64;Component
 * public class AppKeyboardConfigurer implements KeyboardConfigurer {
 *
 * 	&#64;Override
 * 	public void configure(KeyboardConfig config) {
 * 		config.register(MyanmarLayout.create())
 * 				.layouts(List.of("qwerty", "myanmar"))
 * 				.layoutFor(PasswordField.class, "numeric")
 * 				.keyHeight(60);
 * 	}
 * }
 * </pre>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>Values set here override the properties and are overridden by
 * {@code @Keyboard} on a field.</li>
 * <li>If several configurers exist, the order they run in is not defined, so
 * keep them independent.</li>
 * <li>After all configurers ran, the global settings are validated and startup
 * fails if a layout id is unknown.</li>
 * </ul>
 *
 * @author SaiZawMyint
 * @see KeyboardConfig
 * @see io.github.itech_framework.java_fx.annotations.Keyboard
 * @since 1.1.0
 */
public interface KeyboardConfigurer {

	/**
	 * Changes the global keyboard settings.
	 *
	 * @param config the global settings (never {@code null})
	 */
	void configure(KeyboardConfig config);
}