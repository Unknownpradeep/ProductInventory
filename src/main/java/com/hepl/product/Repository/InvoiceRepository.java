package com.hepl.product.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrderCode(String orderCode);
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    boolean existsByOrderCode(String orderCode);

    @Query("SELECT i FROM Invoice i WHERE i.deleted = false AND " +
           "(:search IS NULL OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.orderCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR LOWER(i.status) = LOWER(:status)) AND " +
           "(:customerId IS NULL OR i.customer.id = :customerId)")
    Page<Invoice> searchAndFilter(
        @Param("search") String search,
        @Param("status") String status,
        @Param("customerId") Long customerId,
        Pageable pageable
    );
}
