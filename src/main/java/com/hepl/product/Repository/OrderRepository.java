package com.hepl.product.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    Order findByOrderCode(String orderCode);
    List<Order> findByStatus(String status);
    Page<Order> findByDeletedFalse(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND " +
           "(:search IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR LOWER(o.status) = LOWER(:status)) AND " +
           "(:paymentStatus IS NULL OR LOWER(o.paymentstatus) = LOWER(:paymentStatus)) AND " +
           "(:customerId IS NULL OR o.customer.id = :customerId)")
    Page<Order> searchAndFilter(
        @Param("search") String search,
        @Param("status") String status,
        @Param("paymentStatus") String paymentStatus,
        @Param("customerId") Long customerId,
        Pageable pageable
    );
}
