package com.hepl.product.Service;

import java.util.List;

import com.hepl.product.model.Role;

public interface RoleService {
    List<Role> listAll();
    Role get(Long id);
    Role save(Role role);
    Role update(Long id, Role role);
    void delete(Long id);
    Role assignPermissions(Long roleId, List<Long> permissionIds);
}
