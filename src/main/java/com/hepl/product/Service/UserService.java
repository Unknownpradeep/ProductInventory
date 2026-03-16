package com.hepl.product.Service;

import java.util.List;

import com.hepl.product.model.User;

public interface UserService {


    
    List<User> listAll();
    User get(Integer id);
    User save(User user);
    User update(Integer id, User user);
    void delete(Integer id);
    User assignRoles(Integer userId, List<Long> roleIds);
}
