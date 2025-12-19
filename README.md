# Clean Architecture with Algebraic Effects in Java

This project demonstrates how to implement Clean Architecture in Java using **algebraic effects** via the [Jiffy](https://github.com/thma/jiffy) library. It shows how algebraic effects provide a principled way to handle side effects while maintaining testability, composability, and separation of concerns.

## Table of Contents

- [What Are Algebraic Effects?](#what-are-algebraic-effects)
- [Why Algebraic Effects for Clean Architecture?](#why-algebraic-effects-for-clean-architecture)
- [How Jiffy Works](#how-jiffy-works)
- [Project Architecture](#project-architecture)
- [Code Walkthrough](#code-walkthrough)
- [Running the Application](#running-the-application)
- [Testing with Effects](#testing-with-effects)

## What Are Algebraic Effects?

Algebraic effects are a programming paradigm that separates the **declaration** of side effects from their **execution**. Instead of directly performing I/O, database calls, or logging, your code describes *what* effects it needs, and a separate handler decides *how* to fulfill them.

### The Traditional Problem

In traditional code, side effects are embedded directly:

```java
public int calculateScore(Long customerId) {
    logger.info("Calculating score...");           // Side effect: logging
    List<Order> orders = orderRepo.findByCustomerId(customerId);  // Side effect: DB
    List<Return> returns = returnRepo.findByCustomerId(customerId); // Side effect: DB
    return computeScore(orders, returns);
}
```

This code is hard to test because:
- You need mocks or test databases
- The method signature doesn't reveal its dependencies
- You can't easily swap implementations

### The Algebraic Effects Solution

With algebraic effects, side effects become *data*:

```java
@Uses({LogEffect.class, OrderRepositoryEffect.class, ReturnRepositoryEffect.class})
public Eff<Integer> calculateScore(Long customerId) {
    return Eff.For(
        log(new LogEffect.Info("Calculating score...")),
        getOrders(customerId),
        getReturns(customerId)
    ).yield((ignored, orders, returns) -> computeScore(orders, returns));
}
```

Key differences:
- **Returns `Eff<Integer>`** instead of `int` - the computation is described, not executed
- **`@Uses` annotation** makes dependencies explicit and enables compile-time checking
- **Effects are values** - `LogEffect.Info` is just a record holding data
- **No execution yet** - the caller decides how and when to run it

## Why Algebraic Effects for Clean Architecture?

Clean Architecture emphasizes:

1. **Independence of frameworks** - Business logic shouldn't depend on specific implementations
2. **Testability** - Business rules can be tested without UI, database, or external services
3. **Independence of UI/Database** - These are details that can be swapped

Algebraic effects achieve this naturally:

| Clean Architecture Goal | How Effects Help |
|------------------------|------------------|
| Dependency inversion | Use cases depend on effect *interfaces*, not implementations |
| Testability | Swap production handlers for test handlers |
| Explicit dependencies | `@Uses` annotations document what a method needs |
| Composability | Effects compose with `flatMap`, `Eff.For`, `Eff.parallel` |

## How Jiffy Works

Jiffy is a Java 21+ library that implements algebraic effects using:

### 1. Effects as Sealed Interfaces

Effects are defined as sealed interfaces with record variants:

```java
public sealed interface LogEffect extends Effect<Void> {
    record Info(String message) implements LogEffect {}
    record Error(String message, Throwable error) implements LogEffect {}
    record Warning(String message) implements LogEffect {}
}
```

- `Effect<T>` declares the return type
- Records hold the effect's parameters
- Sealed interfaces ensure exhaustive pattern matching in handlers

### 2. Effect Handlers

Handlers interpret effects:

```java
// Production handler - uses SLF4J
@Component
public class Slf4jLogHandler implements EffectHandler<LogEffect> {
    private static final Logger logger = LoggerFactory.getLogger("Application");

    @Override
    public <T> T handle(LogEffect effect) {
        if (effect instanceof LogEffect.Info(String message)) {
            logger.info(message);
        } else if (effect instanceof LogEffect.Error(String message, Throwable error)) {
            logger.error(message, error);
        }
        return null;
    }
}

// Test handler - collects logs for assertions
public class CollectingLogHandler implements EffectHandler<LogEffect> {
    private final List<LogEntry> logs = new ArrayList<>();

    @Override
    public <T> T handle(LogEffect effect) {
        if (effect instanceof LogEffect.Info(String message)) {
            logs.add(new LogEntry(LogLevel.INFO, message, null));
        }
        return null;
    }

    public List<LogEntry> getLogs() { return logs; }
}
```

Same effect, different interpretations.

### 3. The Eff Monad

`Eff<T>` is the core type representing an effectful computation:

```java
// Perform a single effect
Eff<Void> logEff = Eff.perform(new LogEffect.Info("Hello"));

// Chain effects with flatMap
Eff<Integer> computation = logEff.flatMap(v -> Eff.pure(42));

// Combine multiple effects with Eff.For (like Scala's for-comprehension)
Eff<Integer> combined = Eff.For(
    getOrders(customerId),
    getReturns(customerId)
).yield((orders, returns) -> computeScore(orders, returns));

// Run effects in parallel
Eff<Pair<List<Order>, List<Return>>> parallel = Eff.parallel(
    getOrders(customerId),
    getReturns(customerId)
);

// Error recovery
Eff<Integer> safe = computation.recover(error -> 0);
```

### 4. Effect Runtime

The runtime wires handlers to effects:

```java
EffectRuntime runtime = EffectRuntime.builder()
    .withHandlerUnsafe(LogEffect.class, new Slf4jLogHandler())
    .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, orderHandler)
    .withHandlerUnsafe(ReturnRepositoryEffect.FindByCustomerId.class, returnHandler)
    .build();

// Execute the computation
Integer result = myEffectfulComputation.runWith(runtime);
```

### 5. Compile-Time Effect Checking with @Uses

The `@Uses` annotation declares which effects a method uses:

```java
@Uses({LogEffect.class, OrderRepositoryEffect.class})
public Eff<Integer> calculateScore(Long customerId) { ... }
```

Jiffy's annotation processor validates at compile time that:
- All effects used in the method body are declared in `@Uses`
- Callers of this method also declare the required effects

This catches missing dependencies at compile time, not runtime.

## Project Architecture

```
src/main/java/jiffy_clean_architecture/
├── domain/                    # Pure domain entities (no effects)
│   ├── Customer.java         # Contains pure business logic
│   ├── Order.java
│   └── Return.java
├── usecases/                  # Effect definitions + use case logic
│   ├── CustomerScoreUseCase.java   # Business logic using effects
│   ├── LogEffect.java              # Logging effect definition
│   ├── OrderRepositoryEffect.java  # Order repository effect definition
│   └── ReturnRepositoryEffect.java # Return repository effect definition
├── adapters/                  # Effect handlers (implementations)
│   ├── Slf4jLogHandler.java              # Production logging
│   ├── CollectingLogHandler.java         # Test logging
│   ├── InMemoryOrderRepositoryHandler.java
│   └── InMemoryReturnRepositoryHandler.java
└── application/               # Spring Boot integration
    ├── CustomerScoreApplication.java  # App config + runtime setup
    └── CustomerScoreController.java   # REST endpoints
```

### Layer Dependencies

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  (CustomerScoreController, CustomerScoreApplication)    │
│         Depends on: usecases, adapters, domain          │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     Adapters Layer                       │
│    (Slf4jLogHandler, InMemoryOrderRepositoryHandler)    │
│              Depends on: usecases, domain               │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Use Cases Layer                       │
│        (CustomerScoreUseCase, Effect interfaces)        │
│                  Depends on: domain                     │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│              (Customer, Order, Return)                  │
│                 No dependencies                         │
└─────────────────────────────────────────────────────────┘
```

## Code Walkthrough

### Step 1: Define Domain Entities

Pure domain logic with no dependencies:

```java
public class Customer {
    private final long id;

    public int calculateScore(List<Order> orders, List<Return> returns) {
        BigDecimal totalOrderValue = orders.stream()
            .map(Order::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnValue = returns.stream()
            .map(Return::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Score = ((orders - returns) / orders) * 100
        return totalOrderValue.subtract(totalReturnValue)
            .divide(totalOrderValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .intValue();
    }
}
```

### Step 2: Define Effects

Declare what operations your use cases need:

```java
public sealed interface OrderRepositoryEffect<T> extends Effect<T> {
    record FindByCustomerId(Long customerId)
        implements OrderRepositoryEffect<List<Order>> {}

    record FindById(Long orderId)
        implements OrderRepositoryEffect<Optional<Order>> {}

    record Save(Order order)
        implements OrderRepositoryEffect<Order> {}
}
```

### Step 3: Implement Use Cases

Compose effects to implement business logic:

```java
public class CustomerScoreUseCase {

    @Uses({LogEffect.class, OrderRepositoryEffect.class, ReturnRepositoryEffect.class})
    public Eff<Integer> calculateScore(Long customerId) {
        return Eff.For(
            log(new LogEffect.Info("Calculating score for customer " + customerId)),
            getOrders(customerId),
            getReturns(customerId)
        ).yieldEff((ignored, orders, returns) -> {
            // Pure domain logic
            Customer customer = new Customer(customerId);
            int score = customer.calculateScore(orders, returns);

            return log(new LogEffect.Info("Customer " + customerId + " has score " + score))
                .map(v -> score);
        });
    }

    // Alternative: parallel execution
    @Uses({LogEffect.class, OrderRepositoryEffect.class, ReturnRepositoryEffect.class})
    public Eff<Integer> calculateScoreParallel(Long customerId) {
        return log(new LogEffect.Info("Calculating score..."))
            .flatMap(ignored ->
                Eff.parallel(
                    getOrders(customerId),
                    getReturns(customerId)
                ).flatMap(pair -> {
                    int score = new Customer(customerId)
                        .calculateScore(pair.getFirst(), pair.getSecond());
                    return log(new LogEffect.Info("Score: " + score))
                        .map(v -> score);
                })
            );
    }

    @Uses(OrderRepositoryEffect.class)
    private Eff<List<Order>> getOrders(Long customerId) {
        return Eff.perform(new OrderRepositoryEffect.FindByCustomerId(customerId));
    }
}
```

### Step 4: Implement Handlers

Provide concrete implementations:

```java
@Component
public class InMemoryOrderRepositoryHandler implements EffectHandler<OrderRepositoryEffect<?>> {
    private final Map<Long, List<Order>> ordersByCustomer = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T handle(OrderRepositoryEffect<?> effect) {
        if (effect instanceof OrderRepositoryEffect.FindByCustomerId(Long customerId)) {
            return (T) ordersByCustomer.getOrDefault(customerId, Collections.emptyList());
        } else if (effect instanceof OrderRepositoryEffect.Save(Order order)) {
            // save logic...
            return (T) order;
        }
        throw new IllegalArgumentException("Unknown effect: " + effect);
    }
}
```

### Step 5: Wire and Execute

Configure the runtime and execute effects:

```java
@RestController
public class CustomerScoreController {
    private final CustomerScoreUseCase useCase;
    private final EffectRuntime runtime;

    @GetMapping("/customers/{id}/score")
    public ResponseEntity<ScoreResponse> getScore(@PathVariable Long id) {
        // Create the effect computation (nothing executed yet)
        Eff<Integer> scoreEffect = useCase.calculateScore(id);

        // Execute with the runtime
        Integer score = scoreEffect.runWith(runtime);

        return ResponseEntity.ok(new ScoreResponse(id, score, "Success"));
    }
}
```

## Running the Application

### Prerequisites

- Java 21+
- Maven 3.9+
- Spring Boot 4.0 (included via parent pom)

### Build and Test

```bash
# Build and run all tests
mvn clean install

# Run tests only
mvn test

# Run a single test
mvn test -Dtest=ComputeScoreUseCaseTest
```

### Run the Application

```bash
mvn spring-boot:run
```

### API Endpoints

```bash
# Calculate customer score
curl http://localhost:8080/customers/1/score

# Calculate with error recovery
curl http://localhost:8080/customers/1/score-safe
```

## Testing with Effects

The power of algebraic effects shines in testing. Swap production handlers for test handlers:

```java
@Test
public void testComputeScore() {
    // Create test handlers
    var orderHandler = new InMemoryOrderRepositoryHandler();
    var returnHandler = new InMemoryReturnRepositoryHandler();
    var logHandler = new CollectingLogHandler();

    // Set up test data
    orderHandler.addOrdersForCustomer(1L, List.of(
        new Order(1L, new BigDecimal("100.00")),
        new Order(2L, new BigDecimal("50.00"))
    ));

    // Build test runtime
    var runtime = EffectRuntime.builder()
        .withHandlerUnsafe(LogEffect.class, logHandler)
        .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, orderHandler)
        .withHandlerUnsafe(ReturnRepositoryEffect.FindByCustomerId.class, returnHandler)
        .build();

    // Execute and verify
    int score = useCase.calculateScore(1L).runWith(runtime);

    assertEquals(100, score);
    assertTrue(logHandler.containsMessagePart("Calculating score"));
}
```

No mocks needed. The same use case code runs with different handlers.

## Key Takeaways

1. **Effects as data** - Side effects become first-class values that can be composed, transformed, and inspected
2. **Separation of concerns** - Use cases declare *what* they need; handlers decide *how* to provide it
3. **Explicit dependencies** - `@Uses` makes dependencies visible and enables compile-time checking
4. **Testability by design** - Swap handlers, not implementations; no mocking frameworks needed
5. **Composability** - Effects compose naturally with `flatMap`, `Eff.For`, and `Eff.parallel`

## Further Reading

- [Jiffy on GitHub](https://github.com/thma/jiffy)
- [Algebraic Effects for the Rest of Us](https://overreacted.io/algebraic-effects-for-the-rest-of-us/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
