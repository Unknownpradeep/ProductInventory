package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hepl.product.Repository.RoleRepository;
import com.hepl.product.Repository.UserRepository;
import com.hepl.product.Service.UserService;
import com.hepl.product.model.Role;
import com.hepl.product.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository repository;
    private final RoleRepository roleRepository;

    @Override
    public List<User> listAll() {
        return repository.findAll();
    }

    @Override
    public User get(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public User save(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return repository.save(user);
    }

    @Override
    public User update(Integer id, User user) {
        User existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        
        existing.setUsername(user.getUsername());
        existing.setFirstname(user.getFirstname());
        existing.setLastname(user.getLastname());
        existing.setEmail(user.getEmail());
        existing.setPhonenumber(user.getPhonenumber());
        existing.setStatus(user.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return repository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public User assignRoles(Integer userId, List<Long> roleIds) {
        User user = repository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        
        List<Role> roles = roleRepository.findAllById(roleIds);
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        
        return repository.save(user);
    }
}
