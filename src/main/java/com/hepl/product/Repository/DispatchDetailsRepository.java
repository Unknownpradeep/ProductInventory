package com.hepl.product.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.DispatchDetails;

@Repository
public interface DispatchDetailsRepository extends JpaRepository<DispatchDetails, Long> {
    Optional<DispatchDetails> findByOrderId(Long orderId);
}
