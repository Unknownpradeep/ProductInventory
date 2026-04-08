package com.hepl.product.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByName(String name);
    Optional<Customer> findByEmail(String email);
    Page<Customer> findByDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "c.deleted = false AND " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:state IS NULL OR LOWER(c.state) LIKE LOWER(CONCAT('%', :state, '%'))) AND " +
           "(:country IS NULL OR LOWER(c.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    Page<Customer> searchAndFilter(
        @Param("search") String search,
        @Param("name") String name,
        @Param("email") String email,
        @Param("state") String state,
        @Param("country") String country,
        Pageable pageable
    );
}
