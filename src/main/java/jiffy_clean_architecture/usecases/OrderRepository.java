package jiffy_clean_architecture.usecases;

import jiffy_clean_architecture.domain.Order;

import java.util.List;

public interface OrderRepository {
    List<Order> findOrdersByCustomerId(Long customerId);
}
