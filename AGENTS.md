# Repository Guidelines

## Project Structure & Module Organization

This repository builds a Java 17 Keycloak 26.4.7 provider as a single Maven JAR.

- `src/main/java/com/aruba/simpl/keycloak/terms/` contains the admin REST resource, terms publication service, hash-based required action, event listener, and their factories.
- `src/main/resources/META-INF/services/` registers Keycloak SPI implementations. Update these files when adding or renaming a provider factory.
- `src/test/java/.../terms/` mirrors the production package and contains unit tests.
- `compose.yaml` runs a development Keycloak and mounts the JAR from `target/`.
- `README.md`, `spec.md`, and `tc-hash-spec.md` describe setup and intended behavior.

Keep the publication API and hash-enforcement flow conceptually separate: they are currently independent experiments, as documented in `README.md`.

## Build, Test, and Development Commands

Use the checked-in Maven wrapper rather than requiring a global Maven installation.

- `./mvnw clean package` compiles, runs all tests, and creates the provider JAR.
- `./mvnw test` runs the JUnit test suite only.
- `./mvnw -Dtest=TermsHashProvidersTest test` runs one test class.
- `docker compose up -d` starts Keycloak after the JAR has been built.
- `docker compose restart keycloak` reloads a rebuilt provider; `docker compose logs -f keycloak` follows startup logs.
- `docker compose down` stops the local instance while retaining its named volume.

## Coding Style & Naming Conventions

Follow existing Java conventions: four-space indentation, braces on the same line, one public top-level type per file, and package names in lowercase. Use `PascalCase` for types, `camelCase` for methods and variables, and `UPPER_SNAKE_CASE` for constants. Prefer small, final provider classes and descriptive provider IDs. There is no configured formatter or linter; keep imports organized and remove trailing whitespace before committing. Never log credentials or access tokens; abbreviate hashes where practical.

## Testing Guidelines

Tests use JUnit 5 and Mockito. Name classes `*Test` and test methods as behavior statements, for example `addsTermsActionWhenTheAcceptedHashDiffers`. Cover both the success path and skipped/error conditions. Mock Keycloak interfaces instead of starting a server for unit tests. No coverage threshold is configured, but every behavior change should include focused regression tests. Run `./mvnw clean package` before opening a pull request.

## Commit & Pull Request Guidelines

Recent commits use short, lowercase, imperative subjects such as `improve logging` and `update readme`. Keep each commit focused and explain non-obvious reasoning in the body. Pull requests should summarize behavior changes, link the relevant issue or specification, list verification commands, and call out Keycloak SPI registration or configuration changes. Include screenshots only for user-visible Admin Console changes.
