package com.hepl.product.Service.ServiceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hepl.product.Repository.PermissionRepository;
import com.hepl.product.Service.PermissionService;
import com.hepl.product.model.Permission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    
    private final PermissionRepository repository;

    @Override
    public List<Permission> listAll() {
        return repository.findAll();
    }

    @Override
    public Permission get(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Permission save(Permission permission) {
        return repository.save(permission);
    }

    @Override
    public Permission update(Long id, Permission permission) {
        Permission existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Permission Not Found"));
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
