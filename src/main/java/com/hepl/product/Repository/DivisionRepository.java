package com.hepl.product.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Division;

@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {
    Optional<Division> findByName(String name);
    Optional<Division> findByNameIgnoreCase(String name);
    Page<Division> findByDeletedFalse(Pageable pageable);

    @Query("SELECT d FROM Division d WHERE d.deleted = false AND " +
           "(:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Division> searchAndFilter(@Param("search") String search, Pageable pageable);
}
