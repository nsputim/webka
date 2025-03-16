# CLAUDE.md - Project Guidelines

## Build Commands
- Build project: `./gradlew build`
- Build debug APK: `./gradlew app:assembleDebug`
- Build release APK: `./gradlew app:assembleRelease`

## Test & Lint Commands
- Run all tests: `./gradlew test`
- Run single test: `./gradlew app:testDebugUnitTest --tests "com.smr.web.ExampleUnitTest"`
- Run instrumented tests: `./gradlew connectedAndroidTest`
- Lint checks: `./gradlew lint`

## Code Style Guidelines
- Follow official Kotlin style guide
- Naming: classes=PascalCase, functions/variables=camelCase, constants=UPPER_SNAKE_CASE
- Package structure: feature-based (com.smr.web.[feature])
- Architecture: MVVM with Android Jetpack components
- Documentation: KDoc style for public APIs
- Error handling: Use Result<T> for operations that can fail, with appropriate logging
- Imports: Group by standard library, Android framework, and third-party libraries
- Use ViewBinding for UI interactions
- Log with TAG constants (typically class name) for debugging