package jiffy_clean_architecture.usecases;

import jiffy_clean_architecture.domain.Return;

import java.util.List;

public interface ReturnRepository {
    List<Return> findReturnsByCustomerId(Long customerId);
}
