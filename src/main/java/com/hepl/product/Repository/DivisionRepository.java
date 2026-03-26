package com.hepl.product.Repository;



import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.Division;


@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {
   Optional<Division> findByName(String name);
}
