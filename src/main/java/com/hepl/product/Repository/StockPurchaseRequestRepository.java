package com.hepl.product.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.StockPurchaseRequest;

@Repository
public interface StockPurchaseRequestRepository extends JpaRepository<StockPurchaseRequest, Long> {
    List<StockPurchaseRequest> findAllByOrderByCreatedAtDesc();
    List<StockPurchaseRequest> findByRequestedByOrderByCreatedAtDesc(String requestedBy);
    List<StockPurchaseRequest> findByStatusOrderByCreatedAtDesc(String status);
}
