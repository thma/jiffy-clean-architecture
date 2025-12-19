package jiffy_clean_architecture_test;

import jiffy_clean_architecture.usecases.CustomerScoreUseCase;
import jiffy_clean_architecture.usecases.LogEffect;
import jiffy_clean_architecture.usecases.OrderRepositoryEffect;
import jiffy_clean_architecture.usecases.ReturnRepositoryEffect;
import jiffy_clean_architecture.adapters.CollectingLogHandler;
import jiffy_clean_architecture.adapters.InMemoryOrderRepositoryHandler;
import jiffy_clean_architecture.adapters.InMemoryReturnRepositoryHandler;
import org.jiffy.core.EffectHandler;
import org.jiffy.core.EffectRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComputeScoreUseCaseTest {

  CustomerScoreUseCase customerScoreUseCase = new CustomerScoreUseCase();

  @Test
  public void testComputeScore() {
    // Given
    long customerId = 1L;

    var orderHandler = new InMemoryOrderRepositoryHandler();
    var returnHandler = new InMemoryReturnRepositoryHandler();
    var logHandler = new CollectingLogHandler();

    // Build runtime with test handlers
    var runtime = EffectRuntime.builder()
            .withHandlerUnsafe(LogEffect.class, logHandler)
            .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, orderHandler)
            .withHandlerUnsafe(ReturnRepositoryEffect.FindByCustomerId.class, returnHandler)
            .build();

    // When
    int score = customerScoreUseCase.calculateScore(customerId).runWith(runtime);

    // Then
    assertEquals(100, score);
  }

  @Test
  public void testCalculateScoreWithRecovery_success() {
    // Given
    long customerId = 1L;

    var orderHandler = new InMemoryOrderRepositoryHandler();
    var returnHandler = new InMemoryReturnRepositoryHandler();
    var logHandler = new CollectingLogHandler();

    var runtime = EffectRuntime.builder()
            .withHandlerUnsafe(LogEffect.class, logHandler)
            .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, orderHandler)
            .withHandlerUnsafe(ReturnRepositoryEffect.FindByCustomerId.class, returnHandler)
            .build();

    // When
    int score = customerScoreUseCase.calculateScoreWithRecovery(customerId).runWith(runtime);

    // Then - should succeed with normal score calculation
    assertEquals(100, score);
    assertEquals(0, logHandler.getCountByLevel(CollectingLogHandler.LogLevel.ERROR));
  }

  @Test
  public void testCalculateScoreWithRecovery_recoversOnError() {
    // Given
    long customerId = 1L;

    var logHandler = new CollectingLogHandler();

    // Create a failing order handler
    EffectHandler<OrderRepositoryEffect<?>> failingOrderHandler = new EffectHandler<>() {
      @Override
      public <T> T handle(OrderRepositoryEffect<?> effect) {
        throw new RuntimeException("Database unavailable");
      }
    };

    var returnHandler = new InMemoryReturnRepositoryHandler();

    var runtime = EffectRuntime.builder()
            .withHandlerUnsafe(LogEffect.class, logHandler)
            .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, failingOrderHandler)
            .withHandlerUnsafe(ReturnRepositoryEffect.FindByCustomerId.class, returnHandler)
            .build();

    // When
    int score = customerScoreUseCase.calculateScoreWithRecovery(customerId).runWith(runtime);

    // Then - should recover and return default score of 0
    assertEquals(0, score);
    assertTrue(logHandler.containsMessagePart("Failed to calculate score for customer"));
    assertEquals(1, logHandler.getCountByLevel(CollectingLogHandler.LogLevel.ERROR));
  }
}
