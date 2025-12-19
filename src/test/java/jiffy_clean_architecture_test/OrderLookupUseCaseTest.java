package jiffy_clean_architecture_test;

import jiffy_clean_architecture.adapters.CollectingLogHandler;
import jiffy_clean_architecture.adapters.InMemoryOrderRepositoryHandler;
import jiffy_clean_architecture.domain.Order;
import jiffy_clean_architecture.usecases.LogEffect;
import jiffy_clean_architecture.usecases.OrderLookupUseCase;
import jiffy_clean_architecture.usecases.OrderRepositoryEffect;
import org.jiffy.core.EffectHandler;
import org.jiffy.core.EffectRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class OrderLookupUseCaseTest {

    private OrderLookupUseCase useCase;
    private InMemoryOrderRepositoryHandler orderHandler;
    private CollectingLogHandler logHandler;

    @BeforeEach
    void setUp() {
        useCase = new OrderLookupUseCase();
        orderHandler = new InMemoryOrderRepositoryHandler();
        logHandler = new CollectingLogHandler();
    }

    private EffectRuntime buildRuntime() {
        return EffectRuntime.builder()
                .withHandlerUnsafe(LogEffect.class, logHandler)
                .withHandlerUnsafe(OrderRepositoryEffect.FindById.class, orderHandler)
                .build();
    }

    @Test
    void lookupOrder_whenOrderExists_shouldReturnOrderAndLogIt() {
        // Given
        Order expectedOrder = new Order(1L, new BigDecimal("100.00"));
        orderHandler.addOrdersForCustomer(99L, List.of(expectedOrder));

        // When
        Optional<Order> result = useCase.lookupOrder(1L).runWith(buildRuntime());

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedOrder, result.get());
        assertTrue(logHandler.containsMessagePart("Found order:"));
        assertEquals(1, logHandler.getCountByLevel(CollectingLogHandler.LogLevel.INFO));
    }

    @Test
    void lookupOrder_whenOrderNotFound_shouldReturnEmptyAndLogNotFound() {
        // Given - no orders in the handler

        // When
        Optional<Order> result = useCase.lookupOrder(999L).runWith(buildRuntime());

        // Then
        assertTrue(result.isEmpty());
        assertTrue(logHandler.containsMessagePart("No order found for ID 999"));
        assertEquals(1, logHandler.getCountByLevel(CollectingLogHandler.LogLevel.INFO));
    }

    @Test
    void lookupOrder_whenErrorOccurs_shouldRecoverAndLogError() {
        // Given - a handler that throws an exception
        EffectHandler<OrderRepositoryEffect<?>> failingHandler = new EffectHandler<>() {
            @Override
            public <T> T handle(OrderRepositoryEffect<?> effect) {
                throw new RuntimeException("Database connection failed");
            }
        };

        var runtime = EffectRuntime.builder()
                .withHandlerUnsafe(LogEffect.class, logHandler)
                .withHandlerUnsafe(OrderRepositoryEffect.FindById.class, failingHandler)
                .build();

        // When
        Optional<Order> result = useCase.lookupOrder(1L).runWith(runtime);

        // Then
        assertTrue(result.isEmpty());
        assertTrue(logHandler.containsMessagePart("Failed to lookup order 1"));
        assertEquals(1, logHandler.getCountByLevel(CollectingLogHandler.LogLevel.ERROR));
    }
}
