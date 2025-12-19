package jiffy_clean_architecture_test;

import jiffy_clean_architecture.domain.Customer;
import jiffy_clean_architecture.domain.Order;
import jiffy_clean_architecture.domain.Return;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComputeScoreDomainTest {

  @Test
  public void test100ScoreForCustomerWithNoOrdersOrReturns() {
    // Given
    Customer customer = new Customer(1L);

    // When
    List<Order> orders = List.of(); // Assuming we have an empty list of orders for this test
    List<Return> returns = List.of(); // Assuming we have an empty list of returns for this test

    // Then
    assertEquals(100, customer.calculateScore(orders,returns));
  }

  @Test
  public void testScoreCalculationWithOrdersAndReturns() {
    // Given
    Customer customer = new Customer(1L);
    List<Order> orders = List.of(
        new Order(1L, BigDecimal.valueOf(100))
    );
    List<Return> returns = List.of(
        new Return(1L, 1L, "Test return", LocalDate.now(), BigDecimal.valueOf(50))
    );

    // When
    int score = customer.calculateScore(orders, returns);

    // Then
    assertEquals(50, score);
  }

  @Test
  public void testScoreCalculationWithMultipleOrdersAndReturns() {
    // Given
    Customer customer = new Customer(1L);
    List<Order> orders = List.of(
        new Order(1L, BigDecimal.valueOf(100)),
        new Order(2L, BigDecimal.valueOf(200)),
        new Order(3L, BigDecimal.valueOf(100)),
        new Order(4L, BigDecimal.valueOf(100)),
        new Order(5L, BigDecimal.valueOf(500))
    );
    List<Return> returns = List.of(
        new Return(1L, 5L, "Test return", LocalDate.now(), BigDecimal.valueOf(500))
    );

    // When
    int score = customer.calculateScore(orders, returns);

    // Then
    assertEquals(50, score);
  }
}
