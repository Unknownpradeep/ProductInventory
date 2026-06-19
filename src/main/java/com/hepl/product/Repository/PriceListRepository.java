package com.hepl.product.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.PriceList;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Long> {
    List<PriceList> findAllByOrderByCreatedAtDesc();
    List<PriceList> findByCustomerId(Long customerId);
}
