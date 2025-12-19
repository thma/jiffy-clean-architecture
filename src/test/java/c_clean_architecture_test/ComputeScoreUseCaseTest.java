package c_clean_architecture_test;

import jiffy_clean_architecture.usecases.CustomerScoreUseCase;
import jiffy_clean_architecture.usecases.LogEffect;
import jiffy_clean_architecture.usecases.OrderRepositoryEffect;
import jiffy_clean_architecture.usecases.ReturnRepositoryEffect;
import jiffy_clean_architecture.adapters.CollectingLogHandler;
import jiffy_clean_architecture.adapters.InMemoryOrderRepositoryHandler;
import jiffy_clean_architecture.adapters.InMemoryReturnRepositoryHandler;
import org.jiffy.core.EffectRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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



}
