package com.hepl.product.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hepl.product.model.Login;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {
   Optional<Login> findByUsername(String username);
    Optional<Login> findByEmail(String email);
    Optional<Login> findByResetPasswordToken(String resetPasswordToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT l FROM Login l WHERE l.deleted = false OR l.deleted IS NULL ORDER BY l.id DESC")
    List<Login> findByDeletedFalse();
}
