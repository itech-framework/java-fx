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

## Multiple Implementations of One Interface

The core `BeanFactory` is being extended so that several components can implement one interface without a startup error. They can be injected together as a list, with a preferred or ordered selection, using the framework's own annotation names. See the core documentation for the final annotation names.

## Essential External Module Implementations
The framework provides external tools such as `ApiClient` and `JPA`.

To use the application with a REST API:
- API Client 🔥 [api-client](https://github.com/itech-framework/api-client)

To use the application with a database directly:
- JPA 🔥 [itech-jpa](https://github.com/itech-framework/jpa)

Follow the module documentation to use these tools.

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
