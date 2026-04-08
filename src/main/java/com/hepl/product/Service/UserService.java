package com.hepl.product.Service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.hepl.product.model.User;

public interface UserService {
    Page<User> listAll(String search, String username, String email, String status, int page, int size, String sortBy, String sortDir);
    User get(Integer id);
    User save(User user);
    User update(Integer id, User user);
    void delete(Integer id);
    User assignRoles(Integer userId, List<Long> roleIds);
}
