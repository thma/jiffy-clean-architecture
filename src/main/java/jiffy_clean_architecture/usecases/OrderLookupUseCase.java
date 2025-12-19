package jiffy_clean_architecture.usecases;

import jiffy_clean_architecture.domain.Order;
import org.jiffy.annotations.Uses;
import org.jiffy.core.Eff;

import java.util.Optional;

/**
 * Use case for looking up orders by ID with error recovery.
 */
public class OrderLookupUseCase {

    /**
     * Look up an order by ID. Logs the order if found, logs an error on failure.
     *
     * @param orderId the order ID to look up
     * @return an Eff that produces the order wrapped in Optional
     */
    @Uses({LogEffect.class, OrderRepositoryEffect.class})
    public Eff<Optional<Order>> lookupOrder(Long orderId) {
        return findOrderById(orderId)
            .flatMap(orderOpt -> {
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    return log(new LogEffect.Info("Found order: " + order))
                        .map(v -> orderOpt);
                }
                return log(new LogEffect.Info("No order found for ID " + orderId))
                    .map(v -> orderOpt);
            })
            .recoverWith(error ->
                // Log the error and return empty Optional
                log(new LogEffect.Error("Failed to lookup order " + orderId, error))
                    .map(v -> Optional.empty())
            );
    }

    @Uses(LogEffect.class)
    private Eff<Void> log(LogEffect effect) {
        return Eff.perform(effect);
    }

    @Uses(OrderRepositoryEffect.class)
    private Eff<Optional<Order>> findOrderById(Long orderId) {
        return Eff.perform(new OrderRepositoryEffect.FindById(orderId));
    }
}
