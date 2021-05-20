# hilt-conductor

Use Hilt with Conductor Controllers

# Install

Step 1. Add the plugin to your root `build.gradle` file

```groovy
buildscript {
    ...
    dependencies {
        classpath "io.github.funnydevs:hilt-conductor-plugin:<latest_release>"
    }
}
```

Step 2. Apply plugin in application module of `build.gradle`.

```groovy
plugins {
    ...
    id 'dagger.hilt.android.plugin'
}
```

Step 3. Add the dependencies inside your module

```groovy
dependencies {
    implementation 'io.github.funnydevs:hilt-conductor:<latest_release>'
    
    (java)
    annotationProcessor 'io.github.funnydevs:hilt-conductor-processor:<latest_release>'
    (kotlin)
    kapt 'io.github.funnydevs:hilt-conductor-processor:<latest_release>'
}
```

# Usage

1. Add @ConductorEntryPoint annotation to your Controller
```kotlin
@ConductorEntryPoint
class MainController(args: Bundle?) : Controller(args)
```
2. Install your dagger module in @ControllerComponent
```kotlin
@InstallIn(ControllerComponent::class)
@Module
object MyModule
```
3. (Optional) Define the scope with @ControllerScoped annotation
```kotlin
@Provides
@ControllerScoped
fun text(): String = "Hello World"
```


# Todo
- [ ] Handling @Named qualifier
