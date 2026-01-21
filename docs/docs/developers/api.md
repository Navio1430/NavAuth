# NavAuth API

Here we will cover how to use the NavAuth API.

## Adding the Dependency

Include the NavAuth API in your Maven or Gradle build files.

### Maven
You need to update your .m2/settings.xml file. Please check [GitHub Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry) page for more info.

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/Navio1430/NavAuth</url>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository>
```
```xml
<dependency>
  <groupId>pl.spcode.navauth</groupId>
  <artifactId>navauth-api</artifactId>
  <!-- check the latest version on github -->
  <version>0.1.0-SNAPSHOT</version> 
</dependency>
```

### Gradle
You need to update your .gradle/gradle.properties file. Please check [GitHub Gradle registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) page for more info.

#### Gradle (Groovy)
```groovy
repositories {
  maven {
    url = uri("https://maven.pkg.github.com/Navio1430/NavAuth")
    credentials {
      username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
      password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
    }
  }
}

dependencies {
  // check the latest version on github
  compileOnly 'pl.spcode.navauth:navauth-api:0.1.0-SNAPSHOT'
}
```

#### Gradle (Kotlin DSL)
```kotlin
repositories {
  maven {
    url = uri("https://maven.pkg.github.com/Navio1430/NavAuth")
    credentials {
      username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
      password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
    }
  }
}

dependencies {
  // check the latest version on github
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
visit the [Javadoc](https://navio1430.github.io/NavAuth/javadoc/) for the `navauth-api` module.
