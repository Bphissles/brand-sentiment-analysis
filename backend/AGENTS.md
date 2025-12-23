# Repository Guidelines

## Project Structure & Modules
- Backend is a Grails/Gradle app rooted here.
- Core web code lives in `grails-app` (controllers, services, views, domain).
- Supporting Groovy code lives in `src/main/groovy`; tests live in `src/test/groovy` and `src/integration-test/groovy`.
- Frontend assets (JS, CSS, images) are in `grails-app/assets`.

## Build, Test, and Development
- `./gradlew bootRun` – run the application locally.
- `./gradlew test` – run unit tests.
- `./gradlew integrationTest` – run integration tests.
- `./gradlew clean build` – full clean build and run all tests.
- Use the provided `test.sh` script when present for local CI-like runs.

## Coding Style & Naming
- Use Groovy/Java standard conventions: `UpperCamelCase` for classes, `lowerCamelCase` for methods and variables.
- Keep controllers in `grails-app/controllers`, services in `grails-app/services`, and domain classes in `grails-app/domain`.
- Prefer 4-space indentation, no tabs; keep lines reasonably short and readable.
- Follow existing package structures, e.g. `backend.*` and `sentiment.*`.

## Testing Guidelines
- Use Spock/JUnit-style tests under `src/test/groovy` and `src/integration-test/groovy`.
- Mirror the package and class name of the code under test (e.g. `FooServiceSpec` for `FooService`).
- Ensure new features include tests; keep coverage at or above nearby code.
- Run `./gradlew test integrationTest` before opening a PR.

## Commit & Pull Request Guidelines
- Write clear commit messages: short imperative subject line, optional body explaining why (e.g. `Add sentiment scoring thresholds`).
- Group related changes into a single commit when practical; avoid mixing refactors with behavior changes.
- PRs should include a brief description, testing notes (`./gradlew test` etc.), and links to tickets/issues when applicable.
- Include screenshots or API samples for changes that affect responses or UI behavior.

