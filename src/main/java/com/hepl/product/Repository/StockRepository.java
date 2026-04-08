package com.hepl.product.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByProductId(Long productId);

    @Query("SELECT s FROM Stock s WHERE s.deleted = false AND " +
           "(:search IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:type IS NULL OR LOWER(s.type) = LOWER(:type)) AND " +
           "(:productId IS NULL OR s.product.id = :productId)")
    Page<Stock> searchAndFilter(
        @Param("search") String search,
        @Param("type") String type,
        @Param("productId") Long productId,
        Pageable pageable
    );
}