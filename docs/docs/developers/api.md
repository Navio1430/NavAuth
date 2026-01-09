# NavAuth API

Here we will cover how to use the NavAuth API.

## Adding the Dependency

Include the NavAuth API in your Maven or Gradle build files.

### Maven

```xml
<dependency>
  <groupId>pl.spcode.navauth</groupId>
  <artifactId>navauth-api</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle (Groovy)
```groovy
dependencies {
  compileOnly 'pl.spcode.navauth:navauth-api:0.1.0-SNAPSHOT'
}
```

### Gradle (Kotlin DSL)
```kotlin
dependencies {
  compileOnly("pl.spcode.navauth:navauth-api:0.1.0-SNAPSHOT")
}
```

## Velocity Plugin Dependency

Declare NavAuth as a plugin dependency in your main class using the `@Plugin` annotation.

```java
@Plugin(
    id = "navauthexamples",
    name = "NavAuthExamples",
    // ...
    dependencies = {@Dependency(id = "navauth")})
public class Main implements NavAuthEventListener {
  // ...
}
```

## Registering Event Listeners

Access the event bus via `NavAuthAPI.getInstance().getEventBus()` and register listener instances.

```java
public void registerListeners() {
  // get the NavAuth API instance
  var api = NavAuthAPI.getInstance();
  // access the event bus and register your listener
  api.getEventBus().register(NavAuthListeners());
}
```

```java
// make sure your class implements NavAuthEventListener
public class UserAuthenticatedListener implements NavAuthEventListener {

  // remember to use the pl.spcode.navauth.api.event.Subscribe annotation
  @Subscribe
  public void onUserAuthenticatedEvent(UserAuthenticatedEvent event) {
    // ...
  }
}
```

## Check available events in the API javadoc

If you want to check what events are available in the API,
visit the [Javadoc](../../javadoc/) for the `navauth-api` module.
