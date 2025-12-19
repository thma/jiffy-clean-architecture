package jiffy_clean_architecture.adapters;

import jiffy_clean_architecture.domain.Return;
import jiffy_clean_architecture.usecases.ReturnRepositoryEffect;
import org.jiffy.core.EffectHandler;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * In-memory implementation of ReturnRepositoryEffect handler for testing.
 */
@Component
public class InMemoryReturnRepositoryHandler implements EffectHandler<ReturnRepositoryEffect<?>> {

    private final Map<Long, List<Return>> returnsByCustomer;
    private final Map<Long, Return> returnsById;
    private long nextId = 1;

    public InMemoryReturnRepositoryHandler() {
        this.returnsByCustomer = new HashMap<>();
        this.returnsById = new HashMap<>();
    }

    public InMemoryReturnRepositoryHandler(Map<Long, List<Return>> initialData) {
        this.returnsByCustomer = new HashMap<>(initialData);
        this.returnsById = new HashMap<>();

        // Index returns by ID
        for (List<Return> returns : initialData.values()) {
            for (Return returnItem : returns) {
                if (returnItem.getId() != null) {
                    returnsById.put(returnItem.getId(), returnItem);
                    nextId = Math.max(nextId, returnItem.getId() + 1);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T handle(ReturnRepositoryEffect<?> effect) {
        if (effect instanceof ReturnRepositoryEffect.FindByCustomerId(Long customerId)) {
            return (T) findByCustomerId(customerId);
        } else if (effect instanceof ReturnRepositoryEffect.FindById(Long returnId)) {
            return (T) findById(returnId);
        } else if (effect instanceof ReturnRepositoryEffect.Save(Return returnItem)) {
            return (T) save(returnItem);
        } else if (effect instanceof ReturnRepositoryEffect.DeleteById(Long returnId)) {
            deleteById(returnId);
            return null;
        } else if (effect instanceof ReturnRepositoryEffect.CountByCustomerId(Long customerId)) {
            return (T) countByCustomerId(customerId);
        }
        throw new IllegalArgumentException("Unknown effect: " + effect);
    }

    private List<Return> findByCustomerId(Long customerId) {
        return returnsByCustomer.getOrDefault(customerId, Collections.emptyList());
    }

    private Optional<Return> findById(Long returnId) {
        return Optional.ofNullable(returnsById.get(returnId));
    }

    private Return save(Return returnItem) {
        if (returnItem.getId() == null) {
            // Create new return with generated ID
            Return newReturn = new Return(
                nextId++,
                returnItem.getOrderId(),
                returnItem.getReason(),
                returnItem.getCreatedAt(),
                returnItem.getAmount()
            );
            returnsById.put(newReturn.getId(), newReturn);
            return newReturn;
        } else {
            // Update existing return
            returnsById.put(returnItem.getId(), returnItem);
            return returnItem;
        }
    }

    private void deleteById(Long returnId) {
        Return removed = returnsById.remove(returnId);
        if (removed != null) {
            // Remove from customer mapping
            for (Map.Entry<Long, List<Return>> entry : returnsByCustomer.entrySet()) {
                entry.getValue().removeIf(r -> r.getId().equals(returnId));
            }
        }
    }

    private Long countByCustomerId(Long customerId) {
        return (long) returnsByCustomer.getOrDefault(customerId, Collections.emptyList()).size();
    }


    /**
     * Clear all data.
     */
    public void clear() {
        returnsByCustomer.clear();
        returnsById.clear();
        nextId = 1;
    }
}