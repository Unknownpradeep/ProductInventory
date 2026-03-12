package com.hepl.product.Service;

import java.util.List;

import com.hepl.product.model.Permission;

public interface PermissionService {
    List<Permission> listAll();
    Permission get(Long id);
    Permission save(Permission permission);
    Permission update(Long id, Permission permission);
    void delete(Long id);
}
