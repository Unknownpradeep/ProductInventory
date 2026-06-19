package com.hepl.product.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.ItemGroupProduct;

@Repository
public interface ItemGroupProductRepository extends JpaRepository<ItemGroupProduct, Long> {
    Optional<ItemGroupProduct> findByItemGroupIdAndProductId(Long groupId, Long productId);
    void deleteByItemGroupIdAndProductId(Long groupId, Long productId);
}
