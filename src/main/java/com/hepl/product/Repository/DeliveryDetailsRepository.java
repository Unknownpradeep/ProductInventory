package com.hepl.product.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.DeliveryDetails;

@Repository
public interface DeliveryDetailsRepository extends JpaRepository<DeliveryDetails, Long> {
    Optional<DeliveryDetails> findByOrderId(Long orderId);
}
