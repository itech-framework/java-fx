# FlexiTech Fx Framework - JavaFx Module 🌱

Modern JavaFX foundation for the FlexiTech Fx Framework, providing essential tools
for building rich desktop interfaces.

## Features ✨
- MVVM pattern implementation support
- Built-in dependency injection (`@Use`, shared with the core framework)
- FX controllers and FX components
- Routers
- Router Configs - middlewares and transitions
- Seamless Java 17+ integration (Records, sealed hierarchies, etc.)
- Multi-threaded task management utilities
- Modular theme switching capabilities
- Built-in AlertDialogs, ModalDialogs and LoadingDialogs
- Toast notifications with fixed-width stacking, style variants and progress support
- Global application (OS-level) notifications, started automatically by the framework
- Startup preloader and smooth scrolling
- On-screen (touch) virtual keyboard built only from public JavaFX APIs, with multiple language layouts
- Default stylesheet bundled with the library, fully overridable with your own CSS

## What's New 🆕
- **Core alignment**: the module now follows the new core conventions. `@Rx` is replaced by `@Use`, and `@BusinessLogic` by `@Service`. Fields still annotated with `@Rx` are no longer injected.
- **Toast redesign**:
  - Fixed width, so stacked toasts align instead of sizing to their content.
  - The close icon floats at the top-right corner instead of sitting after the title.
  - Visual style variants.
  - Progress toasts, reusable for long-running work such as uploads and downloads.
  - Styling is CSS-driven, with defaults in the bundled `modern-styles.css`.
- **Dialogs**: `AbstractFxDialog`, `AlertDialog`, `LoadingDialog`, `ModalDialog` and `OwnerEffectGuard` were reviewed for bugs and styling. Dialog styling now also comes from the bundled stylesheet.
- **Global notifications**: an OS-level notification system that starts automatically with the framework, so the application needs no bootstrap code. Its defaults live in `FxSettings` under the `fx.notification.*` keys, and it is extensible, so modules such as calendar events, tasks or Drive can plug in their own reminders.
- **Multiple implementations of one interface**: the core `BeanFactory` is being extended so several implementations of the same interface no longer fail at startup, can be injected as a list, and support preferred and ordered selection.
- **Virtual keyboard**: an opt-in on-screen keyboard for touch devices. It needs no `--add-exports` and no `com.sun.javafx.*` system properties. Layouts are data, so other languages, number pads and custom key orders can be added or replaced. See [Virtual Keyboard](#virtual-keyboard-️).

## Demo Applications
- [Daily Planner Application - Framework version 1.0.11+Latest](https://github.com/itech-framework/daily-planner)
- [Task Management Application - Framework version 1.0.1](https://github.com/itech-framework/demo)

## Getting Started 🚀

### Requirements
- Java 17+

### Installation
Add to your project via Maven:
```xml
<dependencies>
    <dependency>
        <groupId>io.github.itech-framework</groupId>
        <artifactId>core</artifactId>
        <version>${framework-version}</version>
    </dependency>

    <dependency>
        <groupId>io.github.itech-framework</groupId>
        <artifactId>java-fx</artifactId>
        <version>${framework-version}</version>
    </dependency>
</dependencies>
```

Add the other JavaFX controls and graphics dependencies your application needs.

### Explanation Of Example Usage
- Create the main application class:
```java
@ComponentScan(basePackage = "org.itech.framework.test")
public class Main extends ITechJavaFxApplication {
    private static final Logger log = LogManager.getLogger(Main.class);

    @Override
    public void onInit() {
        RouterConfig.setDarkModeKey("isDarkMode");

        router.getConfig().addStyleSheets(Objects.requireNonNull(getClass().getResource("/static/css/app.css")).toExternalForm());

        router.registerRoute("dashboard", "/views/dashboard/dashboard-view.fxml", DashboardController.class);
        router.registerRoute("tasks", "/views/tasks/tasks-view.fxml", TaskViewController.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        router.initialize(Main.class, stage);
        router.to("dashboard");
        stage.show();
    }

    public static void main(String[] args) throws Exception {
        ITechJavaFxApplication.run(Main.class, args);
    }
}
```

`onInit` registers the router config (dark-mode key, application stylesheets) and the pages. `start` initializes the router and redirects to the default page.

- Define a controller class: `DashboardController.java`

```java
@FxController
public class DashboardController implements Routable {
    @Use
    Router router;
}
```
Inside an `@FxController` class, dependencies are injected with `@Use`, exactly as in the core framework.

`@FxController` defines the FXML controller.

`Routable` is the interface that lets the router access the controller. You can handle the following events in a `Routable` class:
```java
public interface Routable {
    default void onNavigate(Object arguments) {}
    default void onReturn(Object result) {}
    default void onResume() {}

    default void preRefresh() {}

    default void postRefresh() {}

    default void refresh() {
        preRefresh();
        postRefresh();
    }

     /**
     * Controller dispose
     */
    default void dispose() {}
}
```
`preRefresh` is called after the FXML reload but before the scene update. `postRefresh` is called after the scene update completes. `refresh` is framework-managed and should not be overridden.

### FxController lifecycle management

```java
@FxController
public class DashboardController implements Routable {
    @Use
    Router router;

    @OnInit
    public void initialized() {
    }

    @PreDestroy
    public void cleanUp() {
    }
}
```

`@OnInit` runs after the FXML and controller are loaded, before the UI update.

`@PreDestroy` is invoked by the framework before the controller is disposed or destroyed.

### Custom FxComponents
The framework supports custom components through the MVVM pattern:

- Define the custom component: `WelcomeWidget.java`
```java
@FxComponent("/views/components/welcome-widget.fxml")
@Getter
public class WelcomeWidget {
    @FXML
    @Root
    private VBox root;

    @FXML
    private Label welcomeTitle;

    private String title;

    @DefaultConstructor
    public WelcomeWidget() {}

    public WelcomeWidget(String title) {
        this.title = title;
    }

    @OnInit
    private void onInit() {
        welcomeTitle.setText(title);
    }
}
```
- Explanation:
  - `@FxComponent` has a `value` attribute, which is always its view URI.
  - The component needs a root node, which the loader recognizes by the `@Root` annotation.
  - If there is a parameterized constructor, define a default constructor with `@DefaultConstructor` so the component processor recognizes it. The parameterized constructor can then be used through the loader:
    ```java
    private void loadWelcomeWidget() {
        WelcomeWidget welcomeWidget = FxComponentLoader.load(WelcomeWidget.class, "Welcome to " + appName);
    }
    ```
  - The component lifecycle is managed with `@OnInit` and `@PreDestroy`, the same as `FxController`.

## Configuration ⚙️

All keys go in `application.properties` (or a profile override such as `application-dev.properties`). Every key has a default, so you only need to set the ones you want to change.

```properties
app.name=My App

fx.app.exit-on-stop=true
fx.app.font.smooth=true
fx.controllers.required=true

fx.theme.enabled=true
fx.theme.stylesheet=/static/css/style.css
fx.theme.dark-mode-key=isDarkMode
fx.theme.dark-style-class=dark-mode
fx.theme.light-fill=#FFFFFF
fx.theme.dark-fill=#000000

fx.preloader.enabled=true
fx.preloader.stylesheet=
fx.preloader.theme=AUTO
fx.preloader.decorated=false
fx.preloader.width=400
fx.preloader.height=200
fx.preloader.bar-width=300
fx.preloader.bar-height=15
fx.preloader.title-font-size=18
fx.preloader.message.starting=Starting...
fx.preloader.message.initializing=Initializing application...
fx.preloader.message.injecting=Injecting data...
fx.preloader.message.preparing=Preparing components...
fx.preloader.message.registering=Registering services...
fx.preloader.message.finalizing=Finalizing...
fx.preloader.message.failed=Startup failed
fx.preloader.delay=0

fx.router.validate-on-init=true
fx.router.run-on-fx-thread=true
fx.router.dispose-policy=ON_LEAVE
fx.router.content-container-id=contentContainer
fx.router.max-redirects=10

fx.components.fxml-base=/views/

fx.background.threads=4
fx.background.scheduled-threads=2
fx.background.daemon=true
fx.background.shutdown-wait-ms=1000

fx.toast.position=BOTTOM_RIGHT
fx.toast.duration-ms=3000
fx.toast.offset-x=20
fx.toast.offset-y=20
fx.toast.max-width=350
fx.toast.max-height=200

fx.scroll.smooth=true
fx.scroll.speed=2.0
fx.scroll.response-ms=80
fx.scroll.content-caching=true

fx.notification.enabled=true
fx.notification.tray=true
fx.notification.tray-icon=
fx.notification.tray-tooltip=Application
fx.notification.poll-seconds=60
fx.notification.max-per-cycle=3
fx.notification.keep-hours=48

fx.keyboard.enabled=false
fx.keyboard.scope=ALL
fx.keyboard.layout=qwerty
fx.keyboard.layouts=
fx.keyboard.anchor-id=
fx.keyboard.min-width=0
fx.keyboard.max-width=0
fx.keyboard.key-height=52
fx.keyboard.key-gap=6
fx.keyboard.max-key-width=96
fx.keyboard.inherit-theme=true
fx.keyboard.style-class=fxvk
fx.keyboard.stylesheet=
```

### Application

| Key | Default | Description |
|-----|---------|-------------|
| `app.name` | `My App` | Application name, used for window titles and the notification tray. |
| `fx.app.exit-on-stop` | `true` | Exit the JVM when the application stops, after the router shutdown and `onShutdown` have run. |
| `fx.app.font.smooth` | `true` | Enable smooth (anti-aliased) font rendering. |
| `fx.controllers.required` | `true` | Every registered route must have a controller class. Startup fails otherwise. |

### Theme

| Key | Default | Description |
|-----|---------|-------------|
| `fx.theme.enabled` | `true` | Turn theme handling (stylesheet and dark mode) on or off. |
| `fx.theme.stylesheet` | `/static/css/style.css` | Classpath path of the main application stylesheet. |
| `fx.theme.dark-mode-key` | `isDarkMode` | Key that holds the dark-mode flag. Same as `RouterConfig.setDarkModeKey(...)`. |
| `fx.theme.dark-style-class` | `dark-mode` | Style class added to the scene root while dark mode is on. Target it in your CSS. |
| `fx.theme.light-fill` | `#FFFFFF` | Scene background fill in light mode. |
| `fx.theme.dark-fill` | `#000000` | Scene background fill in dark mode. |

### Preloader

The preloader is the splash window shown while the framework builds and injects components.

| Key | Default | Description |
|-----|---------|-------------|
| `fx.preloader.enabled` | `true` | Show the preloader during startup. |
| `fx.preloader.stylesheet` | *(empty)* | Optional stylesheet that overrides the preloader's default look. |
| `fx.preloader.theme` | `AUTO` | Preloader theme: follows the app's dark-mode setting when `AUTO`. |
| `fx.preloader.decorated` | `false` | Show the OS window frame (title bar and buttons). |
| `fx.preloader.width` / `height` | `400` / `200` | Window size in pixels. |
| `fx.preloader.bar-width` / `bar-height` | `300` / `15` | Progress bar size in pixels. |
| `fx.preloader.title-font-size` | `18` | Title font size. |
| `fx.preloader.message.starting` | `Starting...` | Message at the start of startup. |
| `fx.preloader.message.initializing` | `Initializing application...` | Message while the application initializes. |
| `fx.preloader.message.injecting` | `Injecting data...` | Message while dependencies are injected. |
| `fx.preloader.message.preparing` | `Preparing components...` | Message while components are prepared. |
| `fx.preloader.message.registering` | `Registering services...` | Message while services are registered. |
| `fx.preloader.message.finalizing` | `Finalizing...` | Message during the last startup step. |
| `fx.preloader.message.failed` | `Startup failed` | Message shown when startup fails. |
| `fx.preloader.delay` | `0` | Extra time in milliseconds to keep the preloader visible after startup finishes. |

### Router

| Key | Default | Description |
|-----|---------|-------------|
| `fx.router.validate-on-init` | `true` | Validate registered routes (FXML and controllers) when the router initializes, so mistakes fail early. |
| `fx.router.run-on-fx-thread` | `true` | Run navigation on the JavaFX Application Thread. |
| `fx.router.dispose-policy` | `ON_LEAVE` | When a page controller is disposed and its `@PreDestroy` methods run. `ON_LEAVE` disposes it as soon as the user navigates away. |
| `fx.router.content-container-id` | `contentContainer` | The `fx:id` of the container where pages are loaded inside a layout. |
| `fx.router.max-redirects` | `10` | Maximum chained redirects (middlewares) per navigation. This stops redirect loops. |

### Components

| Key | Default | Description |
|-----|---------|-------------|
| `fx.components.fxml-base` | `/views/` | Base classpath folder for FXML files. |

### Background Tasks

Used by `BackgroundTaskService` and `TaskScope`.

| Key | Default | Description |
|-----|---------|-------------|
| `fx.background.threads` | `4` | Thread pool size for background tasks. |
| `fx.background.scheduled-threads` | `2` | Thread pool size for scheduled and periodic tasks. |
| `fx.background.daemon` | `true` | Run worker threads as daemon threads so they never block JVM exit. |
| `fx.background.shutdown-wait-ms` | `1000` | Milliseconds to wait for running tasks to finish on shutdown. |

### Toast

| Key | Default | Description |
|-----|---------|-------------|
| `fx.toast.position` | `BOTTOM_RIGHT` | Default screen position. Values come from `ToastPosition`. |
| `fx.toast.duration-ms` | `3000` | How long a toast stays visible, in milliseconds. |
| `fx.toast.offset-x` / `offset-y` | `20` / `20` | Distance from the window edge, in pixels. |
| `fx.toast.max-width` | `350` | Toast width in pixels. |
| `fx.toast.max-height` | `200` | Maximum toast height in pixels. Longer content is clipped. |

### Smooth Scrolling

| Key | Default | Description |
|-----|---------|-------------|
| `fx.scroll.smooth` | `true` | Enable smooth scrolling on scrollable controls. |
| `fx.scroll.speed` | `2.0` | Scroll speed multiplier. Higher scrolls further per wheel step. |
| `fx.scroll.response-ms` | `80` | Smoothing animation time in milliseconds. Lower feels more immediate. |
| `fx.scroll.content-caching` | `true` | Cache scrolled content as a bitmap while scrolling to improve performance. |

### Global Notifications

| Key | Default | Description |
|-----|---------|-------------|
| `fx.notification.enabled` | `true` | Turn the notification system on or off. |
| `fx.notification.tray` | `true` | Show an OS system tray icon. Notifications are sent through the tray. |
| `fx.notification.tray-icon` | *(empty)* | Classpath path of the tray icon image. A default icon is used when empty. |
| `fx.notification.tray-tooltip` | `Application` | Tooltip shown when hovering the tray icon. |
| `fx.notification.poll-seconds` | `60` | How often, in seconds, the framework asks each notification module (calendar, tasks, Drive...) for due reminders. |
| `fx.notification.max-per-cycle` | `3` | Maximum notifications shown per poll cycle. This prevents a burst of popups. |
| `fx.notification.keep-hours` | `48` | How long, in hours, shown notifications are remembered so the same reminder is not shown twice. |

### Virtual Keyboard

| Key | Default | Description |
|-----|---------|-------------|
| `fx.keyboard.enabled` | `false` | Turn the on-screen keyboard on. When it is off nothing is installed and its stylesheet is not loaded. |
| `fx.keyboard.scope` | `ALL` | `ALL` gives every editable text input a keyboard. `ANNOTATED` only gives it to fields with `@Keyboard`. |
| `fx.keyboard.layout` | `qwerty` | Id of the default layout. |
| `fx.keyboard.layouts` | *(empty)* | Comma separated layout ids that the language key cycles through, for example `qwerty,myanmar`. |
| `fx.keyboard.anchor-id` | *(empty)* | `fx:id` of the node whose width the keyboard follows. Empty means the window. |
| `fx.keyboard.min-width` / `max-width` | `0` / `0` | Width limits in pixels. `0` means no limit. `min-width` must not be greater than `max-width`. |
| `fx.keyboard.key-height` | `52` | Key height in pixels (24 to 200). |
| `fx.keyboard.key-gap` | `6` | Gap between keys in pixels (0 to 50). |
| `fx.keyboard.max-key-width` | `96` | Widest a single key unit may grow, in pixels (24 to 400). |
| `fx.keyboard.inherit-theme` | `true` | Also load the theme stylesheet and copy the dark style class into the keyboard. |
| `fx.keyboard.style-class` | `fxvk` | Root style class of the keyboard. |
| `fx.keyboard.stylesheet` | *(empty)* | Optional stylesheet loaded after the bundled `keyboard.css`, so its rules override the defaults. Use a different file name from `keyboard.css`. |

## Toast Notifications 🍞

Toasts are in-app messages (`io.github.itech_framework.java_fx.ui.toast`: `Toast`, `ToastConfig`, `ToastPosition`, `ToastType`).

- Every toast has the same fixed width, so stacked toasts line up.
- The close icon floats at the top-right corner.
- Style variants change the look of a toast independently of its type.
- Progress toasts show ongoing work such as uploads and downloads, and can replace a custom progress panel.
- Defaults (position, duration, offsets, size) come from the `fx.toast.*` keys in [Configuration](#configuration-️).

### Styling with CSS
Toast and dialog appearance is CSS-driven. The library ships a default stylesheet (`modern-styles.css`), so everything looks right with zero configuration. To change the design, add your own stylesheet to the router config and override the same style classes:
```java
router.getConfig().addStyleSheets(Objects.requireNonNull(getClass().getResource("/static/css/app.css")).toExternalForm());
```
Your rules take precedence over the bundled defaults, so you only need to write what you want to change.

## Dialogs

`AlertDialog`, `ModalDialog` and `LoadingDialog` extend `AbstractFxDialog`. They block interaction with the owner window while shown (through `OwnerEffectGuard`) and use the same bundled styles and overrides as toasts. Prefer them over hand-written dialogs for prompts, pickers and loading states.

## Global Application Notifications 🔔

The module includes a global (OS-level) notification system, separate from in-app toasts.

- It is **started automatically** by the framework. No application code is needed to bootstrap it.
- It is configured with the `fx.notification.*` keys in [Configuration](#configuration-️).
- It is **extensible**: modules such as calendar events, tasks or Drive plug in their own reminders. The framework polls them every `fx.notification.poll-seconds`.
- Set `fx.notification.enabled=false` to turn it off completely.

## Virtual Keyboard ⌨️

An on-screen keyboard for touch devices, built only from public JavaFX APIs. It is shown in a popup when a text input gets focus.

- It is **opt-in**: set `fx.keyboard.enabled=true`.
- Its width follows an anchor node you choose, or the window when none is set.
- Layouts are data. You can add languages, change key order or replace a built-in layout.
- Colors come from your theme variables, and dark mode follows the app automatically.

### Quick start

```properties
fx.keyboard.enabled=true
```

With the default scope every editable text input shows the keyboard. Built-in layouts are `qwerty`, `symbols`, `numeric` and `decimal`.

### Choosing which fields get a keyboard

`fx.keyboard.scope=ALL` is the default. To give the keyboard only to selected fields, use `fx.keyboard.scope=ANNOTATED` and add `@Keyboard` to those fields. In both scopes `@Keyboard(enabled = false)` opts a field out.

### Customizing a field with `@Keyboard`

```java
@FxController
public class PaymentController {

    @FXML
    @Keyboard(rows = {"1 2 3", "4 5 6", "7 8 9", "{clear} 0 {back}", "{enter=OK*2} {hide}"})
    private PasswordField pin;

    @FXML
    @Keyboard(layout = "decimal", anchor = "formPane", maxWidth = 360)
    private TextField amount;

    @FXML
    @Keyboard(layouts = {"qwerty", "myanmar"}, keyHeight = 60)
    private TextField customerName;

    @FXML
    @Keyboard(enabled = false)
    private TextField barcode;
}
```

Use only one of `layout`, `rows` or `supplier` per field. Numeric attributes use `-1` for "not set", which keeps the global value. Only `TextInputControl` fields (`TextField`, `PasswordField`, `TextArea`) can be annotated. Wrong usage fails the startup with one message that lists every problem.

### Inline rows syntax

Each string in `rows` is one row. Keys are separated by spaces.

| Token | Meaning |
|-------|---------|
| `a` | A character key. |
| `a/A` | A key with a normal value and a shift value. |
| `{shift}` `{back}` `{enter}` `{space}` `{tab}` `{clear}` `{hide}` | Special keys. |
| `{next}` | Switches to the next layout in the cycle. |
| `{layout:id}` | Switches to a specific layout. |
| `=Label` | Renames a key, for example `{enter=OK}`. |
| `*weight` | Widens a key, for example `{space*5}` is five key units wide. |

Because keys are split on spaces, a space key must be written `{space}`.

### Value order

Values are applied in this order, and the most specific wins: properties, then a `KeyboardConfigurer`, then `@Keyboard` on the field. The layout of one field is chosen in this order: a layout assigned with `KeyboardLayouts.assign(node, id)`, the global `KeyboardLayoutResolver`, then the configured default.

### Global settings in code

```java
@Component
public class AppKeyboardConfigurer implements KeyboardConfigurer {

    @Override
    public void configure(KeyboardConfig config) {
        config.register(MyanmarLayouts.letters())
              .layouts(List.of("qwerty", "myanmar"))
              .layoutFor(PasswordField.class, "numeric")
              .keyHeight(60);
    }
}
```

Make the configurer a normal `@Component` so the core framework injects its fields. If several configurers exist, the order they run in is not defined. After they all ran, the global settings are validated, and an unknown layout id fails the startup.

### Languages and layouts

Add a language with a `KeyboardConfigurer` that calls `register(...)`, or with a `KeyboardLayoutProvider` listed in `META-INF/services/io.github.itech_framework.java_fx.ui.keyboard.KeyboardLayoutProvider`. Registering a layout with the same id as a built-in layout replaces it.

When `fx.keyboard.layouts` (or `@Keyboard(layouts = ...)`) has two or more ids, the keyboard adds a language key next to the space bar on those layouts. Each tap goes to the next layout in the list and wraps around to the first. Layouts that already contain a `{next}` key are left unchanged. Layouts that are not in the list, such as `symbols` and the number pads, have no language key, so they need their own key to go back. Keep the default layout first in the list. The keyboard does not remember the language between fields. Each field starts from its own layout.

### Width and position

The keyboard has no fixed width. It follows an anchor, found in this order:

1. A node assigned with `KeyboardAnchors.assign(...)` on the field or any of its parents.
2. `@Keyboard(anchor = "id")` or `fx.keyboard.anchor-id`.
3. The window.

```java
KeyboardAnchors.assign(formPane, formPane);
KeyboardAnchors.assign(searchField, "searchBar");
```

The width is limited by `min-width`, `max-width`, the natural width of the layout and the screen. The keyboard goes under the anchor when there is room, otherwise inside it at the bottom. With no anchor it goes at the bottom of the window. If it would cover the focused field, it moves above the field. It follows window moves and resizes and anchor size changes.

### Styling

The bundled `keyboard.css` is loaded only when the keyboard is enabled. Load order on the keyboard is: the theme stylesheet (when `fx.keyboard.inherit-theme=true` and the theme is enabled), the bundled `keyboard.css`, then your `fx.keyboard.stylesheet`. Your rules win, so write only what you want to change.

Colors come from theme variables on `.root`: `-fx-surface`, `-fx-control-bg`, `-fx-control-hover`, `-fx-control-pressed`, `-fx-border-base`, `-fx-text-color` and `-fx-primary`. The keyboard carries the style classes `fxvk` and `root`. The dark style class (`fx.theme.dark-style-class`) is copied from the focused scene every time the keyboard is shown. A layout can also have its own style class, for example `keyboard-myanmar`, so you can set a font per language.

```css
.keyboard-myanmar .key-text {
    -fx-font-family: "Myanmar Text", "Noto Sans Myanmar", sans-serif;
    -fx-font-size: 20px;
}
```

If the theme does not define the variables above, define them yourself in your keyboard stylesheet.

### Limitations

- Only `TextInputControl` fields can be annotated. `Spinner` and editable `ComboBox` editors get the keyboard under scope `ALL`, but cannot be configured with `@Keyboard`.
- The Tab key sends a synthetic key event. In a `TextArea` it may insert a tab instead of moving focus.
- The keyboard does not follow a scrolling parent, and it does not scroll the field into view.
- The keyboard stays open when another application window is activated.
- Korean and other composing scripts are not combined by the keyboard. Each key inserts its own character.

## Multiple Implementations of One Interface

The core `BeanFactory` is being extended so that several components can implement one interface without a startup error. They can be injected together as a list, with a preferred or ordered selection, using the framework's own annotation names. See the core documentation for the final annotation names.

## Essential External Module Implementations
The framework provides external tools such as `ApiClient` and `JPA`.

To use the application with a REST API:
- API Client 🔥 [api-client](https://github.com/itech-framework/api-client)

To use the application with a database directly:
- JPA 🔥 [itech-jpa](https://github.com/itech-framework/jpa)

Follow the module documentation to use these tools.

## Upgrading to 1.1.0 ⬆️

Use the same version for the core and every module, and rebuild your application against it.

**Behavior changes**
- Add a Log4j 2 implementation to your application. The module no longer brings `log4j-core`.
- Background services, notifications and the tray icon are now stopped on every `stop()`. `fx.app.exit-on-stop` only decides whether the JVM exits.
- If startup validation fails, the application context is closed before the error is thrown.
- `@ComponentScan` is required next to `@EnableJavaFx`.
- `@OnInit` and `@PreDestroy` methods in a controller's base class now run. Remove manual `super.init()` calls that would run them twice.
- A controller that still uses `@Rx` fails when its first screen loads.
- `FxComponentLoader.load` fails for a class without `@FxComponent`.
- `BackgroundTaskService.executeRunnable` and `scheduleTask` fail their `Future` when the task throws.
- `SystemNotificationService.show` returns a `boolean`.
- A notification that could not be shown is tried again on the next poll.

**Compatible changes**
- `FxThread.call(supplier, timeout, unit)`, `FxTheme.storedDarkMode()` and `FxComponent` with an empty default value.
- The jar has the module name `io.github.itech_framework.java_fx`.
- The virtual keyboard is new and off by default (`fx.keyboard.enabled=false`).

## Migrating from `@Rx`

| Old              | New        |
|------------------|------------|
| `@Rx`            | `@Use`     |
| `@Rx(name = "x")`| `@Use(name = "x")` |

Search your project for `@Rx` before upgrading. Fields still annotated with it are no longer injected.

## 🤝 Support
For assistance:  
📧 Email: `itech.saizawmyint@gmail.com`  
📑 [Open an Issue](https://github.com/itech-framework/java-fx/issues)

---

*Made with ❤️ by the FlexiTech Team*
