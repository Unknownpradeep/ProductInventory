package com.hepl.product.Service.ServiceImpl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hepl.product.Repository.PermissionRepository;
import com.hepl.product.Repository.RoleRepository;
import com.hepl.product.Service.RoleService;
import com.hepl.product.model.Permission;
import com.hepl.product.model.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository repository;
    private final PermissionRepository permissionRepository;

    @Override
    public Page<Role> listAll(String search, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repository.searchAndFilter(search, pageable);
    }


    @Override
    public Role get(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Role save(Role role) {
        return repository.save(role);
    }

    @Override
    public Role update(Long id, Role role) {
        Role existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role Not Found"));
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Role existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role Not Found"));
        existing.setDeleted(true);
        repository.save(existing);
       // repository.deleteById(id); --- IGNORE ---
    }

    @Override
    public Role assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = repository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role Not Found"));
        
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(permissions);
        
        return repository.save(role);
    }
}
