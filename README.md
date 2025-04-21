# FlexiTech Fx Framework - JavaFx Module 🌱

Modern JavaFX foundation for the iTech Framework, providing essential tools
for building rich desktop interfaces.

## Features ✨
- MVVM pattern implementation support
- Built-in dependency injection
- FX controllers and FX components
- Routers
- Router Configs - middlewares and transitions
- Seamless Java 17+ integration (Records, sealed hierarchies, etc.)
- Multi-threaded task management utilities
- Modular theme switching capabilities
- Build-in AlertDialogs and ModalDialogs

## Demo Applications
- [Task Management Application](https://github.com/itech-framework/demo)

## Getting Started 🚀

### Installation
Add to your project via Maven:
```xml
<dependencies>
    <!-- Framework core -->
    <dependency>
        <groupId>io.github.itech-framework</groupId>
        <artifactId>core</artifactId>
        <version>1.0.1</version>
    </dependency>

    <!-- Java Fx -->
    <dependency>
        <groupId>io.github.itech-framework</groupId>
        <artifactId>java-fx</artifactId>
        <version>1.0.1</version>
    </dependency>
    
    <!-- Others Javafx controls,graphics and so on... -->
</dependencies>
```

### Explanation Of Example Usage
- Create main application class:
```java
@ComponentScan(basePackage = "org.itech.framework.test")
public class Main extends ITechJavaFxApplication {
    private static final Logger log = LogManager.getLogger(Main.class);

    @Override
    public void onInit(){
        // defined the router config
        
        // dark-mode support key
        RouterConfig.setDarkModeKey("isDarkMode");

        // application stylesheets
        router.getConfig().addStyleSheets(Objects.requireNonNull(getClass().getResource("/static/css/app.css")).toExternalForm());
        
        // defined pages for router
        router.registerRoute("dashboard", "/views/dashboard/dashboard-view.fxml", DashboardController.class);
        router.registerRoute("tasks", "/views/tasks/tasks-view.fxml", TaskViewController.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // initialized router for your applications
        
        router.initialize(TaskManagerApplication.class, stage);
        
        // default redirect page
        router.to("dashboard");

        stage.show();
    }

    public static void main(String[] args) throws Exception {
        ITechJavaFxApplication.run(TaskManagerApplication.class,args);
    }
}
```

- Defined a controller class: `DashboardController.java`

```java
import io.github.itech_framework.core.annotations.reactives.Rx;

@FxController
public class DashboardController implements Routable {
    // TODO: implement dashboard ui controller

    // dependency injections
    @Rx Router router;
}
```
Inside the `@FxController` class, we can use dependency injection through the component reactor `@Rx` annotations.

`@FxController`: defined the FXML controller.

`Routable`: the interface class that the router can access the controller. You can handle the following features for `Routable` class:
```java
public interface Routable {
    default void onNavigate(Object arguments) {}
    default void onReturn(Object result) {}
    default void onResume(){}
    /**
     * Called after FXML reload but before scene update
     */
    default void preRefresh() {}

    /**
     * Called after scene update completes
     */
    default void postRefresh() {}

    /**
     * Framework-managed refresh (final to prevent override)
     */
    default void refresh() {
        preRefresh();
        postRefresh();
    }
}
```

### FxController lifecycle managements

```java
import io.github.itech_framework.core.annotations.methods.OnInit;
import io.github.itech_framework.core.annotations.methods.PreDestroy;
import io.github.itech_framework.core.annotations.reactives.Rx;
import io.github.itech_framework.java_fx.router.Router;

@FxController
public class DashboardController implements Routable {
    // TODO: implement dashboard ui controller

    // dependency injections
    @Rx
    Router router;

    // the method that fxml and controller is loaded before ui update 
    @OnInit
    public void initialized() {
        // TODO: initialized process
    }

    // the method that the framework will invoke before the controller is disposed or destroy
    @PreDestroy
    public void cleanUp() {
        // TODO: resources clean up
    }
}
```

`@OnInit`: the method that will invoke after fxml and controller is loaded before ui update.

`@PreDestroy`: the method that the framework will invoke before the controller is disposed or destroy

### Custom FxComponents
The framework support custom components through MVVM pattern:

- Defined the custom component: `WelcomeWidget.java`
```java
@FxComponent("/views/components/welcome-widget.fxml")
@Getter
public class WelcomeWidget{
    @FXML
    @Root
    private VBox root;

    @FXML
    private Label welcomeTitle;

    private String title;

    @DefaultConstructor
    public WelcomeWidget(){}

    public WelcomeWidget(String title){
        this.title = title;
    }

    @OnInit
    private void onInit(){
        welcomeTitle.setText(title);
    }
}
```
- Explanation of usages:
  - `@FxComponent` has `value` attribute which is always its view uri.
  - The `FxComponent` need a `root` node that will recognized by the loader. The `root` node is annotated with `@Root` annotation.
  - If there is a parameterize constructor: we need to defined default constructor with `@DefaultConstructor` that will recognized by the component processors. The required parameterize constructor can be load from the loader example:
    ```java
    private void loadWelcomeWidget(){
        WelcomeWidget welcomeWidget = FxComponentLoader.load(WelcomeWidget.class, "Welcome to " + appName);
    }
    ```
  - Component lifecycle can be managed through the `@onInit` and `@PreDestroy` annotations same as the `FxController`.

### Essential External Module Implementations
The framework provided external tools like `ApiClient` and `JPA`.

To use the application with `RESTAPI` you can use 
- API Client 🔥[api-client](https://github.com/itech-framework/api-client)

To use the application with direct databases you can use
- JPA 🔥 [itech-jpa](https://github.com/itech-framework/jpa)

Follow required documentations and you can gracefully use the external tools.

## 🤝 Support
For assistance:  
📧 Email: `itech.saizawmyint@gmail.com`  
📑 [Open an Issue](https://github.com/itech-framework/java-fx/issues)

---

*Made with ❤️ by the FlexiTech Team*