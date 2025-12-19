# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build and run tests
mvn clean install

# Run tests only
mvn test

# Run a single test class
mvn test -Dtest=ComputeScoreUseCaseTest

# Run a single test method
mvn test -Dtest=ComputeScoreUseCaseTest#testComputeScore

# Run the Spring Boot application
mvn spring-boot:run
```

## Architecture

This is a Clean Architecture demo using the [Jiffy](https://github.com/thma/jiffy) algebraic effects library for Java 21+.

### Core Concept: Algebraic Effects

The codebase uses algebraic effects to separate **what** side effects to perform from **how** to execute them:

1. **Effects** (in `usecases/`) - Sealed interfaces that declare operations as data (e.g., `LogEffect`, `OrderRepositoryEffect`). Effects extend `Effect<T>` where T is the return type.

2. **Use Cases** (in `usecases/`) - Business logic that creates effect computations using `Eff.perform()` and composes them with `Eff.For()`, `flatMap()`, etc. Methods are annotated with `@Uses` to declare which effects they require.

3. **Handlers** (in `adapters/`) - Implement `EffectHandler<E>` to interpret effects. Different handlers can be swapped for testing vs production (e.g., `InMemoryOrderRepositoryHandler` vs a real DB handler).

4. **Runtime** - `EffectRuntime` wires handlers together. The controller calls `effect.runWith(runtime)` to execute.

### Layer Structure

```
domain/          - Pure domain entities (Customer, Order, Return)
usecases/        - Effect definitions + use case logic (CustomerScoreUseCase)
adapters/        - Effect handlers (InMemory*, Slf4j*)
application/     - Spring Boot app, controller, runtime config
```

### Key Pattern

Use cases return `Eff<T>` instead of `T`. The controller runs effects:

```java
// In use case - declares effects, doesn't execute them
@Uses({LogEffect.class, OrderRepositoryEffect.class})
public Eff<Integer> calculateScore(Long customerId) { ... }

// In controller - executes with runtime
Integer score = useCase.calculateScore(id).runWith(runtime);
```

### Testing

Tests use in-memory handlers to avoid real I/O:

```java
var runtime = EffectRuntime.builder()
    .withHandlerUnsafe(LogEffect.class, new CollectingLogHandler())
    .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, new InMemoryOrderRepositoryHandler())
    .build();

int score = useCase.calculateScore(1L).runWith(runtime);
```

## REST Endpoints

- `GET /customers/{id}/score` - Calculate customer score
- `GET /customers/{id}/score-safe` - Calculate with error recovery
- `GET /customers/{id}/score-sequential` - Sequential calculation
