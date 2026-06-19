package com.hepl.product.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.ItemGroup;

@Repository
public interface ItemGroupRepository extends JpaRepository<ItemGroup, Long> {
    boolean existsByName(String name);
    List<ItemGroup> findAllByOrderByCreatedAtDesc();
}
