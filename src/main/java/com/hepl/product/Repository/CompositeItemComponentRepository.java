package com.hepl.product.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.CompositeItemComponent;

@Repository
public interface CompositeItemComponentRepository extends JpaRepository<CompositeItemComponent, Long> {
    void deleteByCompositeItemIdAndProductId(Long compositeItemId, Long productId);
}
