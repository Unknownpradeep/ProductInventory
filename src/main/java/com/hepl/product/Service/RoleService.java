package com.hepl.product.Service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.hepl.product.model.Role;

public interface RoleService {
    Page<Role> listAll(String search, int page, int size, String sortBy, String sortDir);
    Role get(Long id);
    Role save(Role role);
    Role update(Long id, Role role);
    void delete(Long id);
    Role assignPermissions(Long roleId, List<Long> permissionIds);
}
