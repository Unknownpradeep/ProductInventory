package com.hepl.product.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
   Optional<Customer> findByName(String name);
   Optional<Customer> findByEmail(String email);
}
