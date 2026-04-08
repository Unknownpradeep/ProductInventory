package com.hepl.product.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Data;

@Data
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String email;
    private Long phonenumber;
    private int roleId;
    private String Status;
    private LocalDateTime lastLogin= LocalDateTime.now();
    private LocalDateTime createdAt= LocalDateTime.now();
    private LocalDateTime updatedAt= LocalDateTime.now();
    private int createdBy;
    private boolean deleted=false;

    @ManyToMany
    @JoinTable(
        name="User_Role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")

       )
    private List<Role> roles;

}
