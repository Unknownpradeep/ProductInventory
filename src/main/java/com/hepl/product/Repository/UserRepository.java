package com.hepl.product.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Page<User> findByDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND " +
           "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstname) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastname) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:status IS NULL OR LOWER(u.Status) = LOWER(:status))")
    Page<User> searchAndFilter(
        @Param("search") String search,
        @Param("username") String username,
        @Param("email") String email,
        @Param("status") String status,
        Pageable pageable
    );
}
