package com.hepl.product.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.CompositeItem;

@Repository
public interface CompositeItemRepository extends JpaRepository<CompositeItem, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<CompositeItem> findAllByOrderByCreatedAtDesc();
}
