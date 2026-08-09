# Form Engine

[![CI](https://github.com/Nc707/form-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/Nc707/form-engine/actions/workflows/ci.yml)

A form engine whose structure is defined at **runtime**, not at compile time. A form definition owns
its fields, their options and dependencies, and a set of per-device layouts; submissions are stored
against those definitions. Java 21, Spring Boot 4.0.2, Maven multi-module, H2 in-memory.

## Modules

The project is sliced by domain (`definition` | `submission`) and then by layer.

| Module | What lives there |
|---|---|
| `form-engine-definition-model` / `form-engine-submission-model` | DTOs, enums, domain exceptions, the specification framework |
| `form-engine-definition-data` / `form-engine-submission-data` | DAO interfaces |
| `form-engine-definition-dataimpl` / `form-engine-submission-dataimpl` | JPA entities, repositories, mappers, DAO implementations |
| `form-engine-definition-business` / `form-engine-submission-business` | Service interfaces |
| `form-engine-definition-businessimpl` / `form-engine-submission-businessimpl` | Service implementations |
| `form-engine-rest` | 7 REST controllers, the error handler, OpenAPI config — port 8080, context path `/form-engine` |
| `form-engine-demo` | Vaadin 25 UI — port 8081, injects the services directly (no HTTP) |

The REST module and the Vaadin demo are two consumers of the same business core.

## Building and running

```bash
mvn clean install                          # full build, runs the integration tests
mvn spring-boot:run -pl form-engine-rest   # REST API on http://localhost:8080/form-engine
mvn spring-boot:run -pl form-engine-demo   # Vaadin UI on http://localhost:8081
```

Two notes on partial builds:

- Always pass `-am` when building a single module (`mvn verify -pl form-engine-rest -am`). Without
  it Maven resolves the sibling modules from `~/.m2` instead of the reactor, and a stale or
  foreign `0.0.1-SNAPSHOT` installed there will be used instead of your working tree.
- `mvn -Pproduction ...` compiles the Vaadin frontend bundle. It is a separate profile on purpose:
  the default build must not need a Node toolchain.

## API documentation

With the REST module running:

- Swagger UI — <http://localhost:8080/form-engine/swagger-ui.html>
- OpenAPI document — <http://localhost:8080/form-engine/v3/api-docs>

Endpoints are grouped under seven tags, all below `/api/v1`:

| Base path | Resource |
|---|---|
| `/api/v1/form-definitions` | Forms: code, title, version |
| `/api/v1/field-definitions` | The fields of a form |
| `/api/v1/field-options` | Options of `SELECT` / `MULTI_SELECT` fields |
| `/api/v1/field-dependencies` | Show/hide/require rules between fields |
| `/api/v1/form-layouts` | Per-device grid layouts, with fallback resolution |
| `/api/v1/form-submissions` | Filled-in instances of a form |
| `/api/v1/field-submissions` | The individual answers inside a submission |

## Error model

Errors are RFC 7807 problem details, served as `application/problem+json` by a single
`@RestControllerAdvice` (`GlobalExceptionHandler`).

| Status | When |
|---|---|
| `400` | Bean Validation failed, a path variable had the wrong type, the body was unreadable, or a domain argument was invalid (an unknown submission status, an id on a create body) |
| `404` | The addressed resource, or a resource it references, does not exist |
| `409` | The form code is already taken |
| `422` | The request is well formed but breaks a domain rule — e.g. a layout referencing fields of another form |
| `500` | Anything unexpected. The detail is generic; the stacktrace goes to the log, never to the client |

```json
{
  "type": "https://form-engine/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Form not found with id: 42",
  "instance": "/api/v1/form-definitions/42",
  "timestamp": "2026-08-09T00:04:11.238Z"
}
```

`400` validation failures and `422` domain failures add an `errors` array — one entry per violated
constraint (`{field, message, rejectedValue}`) or per broken rule (a string).

## Tests

Integration tests live in `form-engine-rest/src/test`, drive the API through `MockMvc` and set up
their data through the API itself. `ErrorHandlingIntegrationTest` and `ValidationIntegrationTest`
pin the status codes and problem bodies; `OpenApiIntegrationTest` guards the springdoc integration,
which is worth having because springdoc is not managed by the Spring Boot BOM.
