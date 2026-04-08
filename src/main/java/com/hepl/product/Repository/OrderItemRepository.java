package com.hepl.product.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByDeletedFalse();
    @Query("""
    SELECT COALESCE(SUM(oi.quantity), 0)
    FROM OrderItem oi
    WHERE oi.product.id = :productId
    AND oi.order.status = "CONFIRMED"
    """)
int getTotalQuantityByProductId(Long productId);
    int getTotalQuantityByProductId(Long productId, String string);
}
