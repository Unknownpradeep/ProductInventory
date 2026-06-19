package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.hepl.product.Repository.RoleRepository;
import com.hepl.product.Repository.UserRepository;
import com.hepl.product.Payload.Dto.UserDto.UserRequestDto;
import com.hepl.product.Payload.Dto.UserDto.UserResponseDto;
import com.hepl.product.model.Role;
import com.hepl.product.model.User;

import lombok.RequiredArgsConstructor;

import com.hepl.product.Service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository repository;
    private final RoleRepository roleRepository;

    @Override
    public Page<UserResponseDto> listAll(String search, String username, String email, String status, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = repository.searchAndFilter(search, username, email, status, pageable);
        return users.map(this::mapToResponse);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponseDto get(Integer id) {
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("User Not Found"));
        return mapToResponse(user);
    }

    @Override
    public UserResponseDto save(UserRequestDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setFirstname(dto.getFirstname());
        user.setLastname(dto.getLastname());
        user.setEmail(dto.getEmail());
        user.setPhonenumber(dto.getPhonenumber());
        user.setStatus(dto.getStatus());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(dto.getRoleIds());
            user.setRoles(roles);
        }
        
        return mapToResponse(repository.save(user));
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDto update(Integer id, UserRequestDto dto) {
        User existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        
        existing.setUsername(dto.getUsername());
        existing.setFirstname(dto.getFirstname());
        existing.setLastname(dto.getLastname());
        existing.setEmail(dto.getEmail());
        existing.setPhonenumber(dto.getPhonenumber());
        existing.setStatus(dto.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(dto.getRoleIds());
            existing.setRoles(roles);
        }
        
        return mapToResponse(repository.save(existing));
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public void delete(Integer id) {
        User existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        existing.setDeleted(true);
        repository.save(existing);
       // repository.deleteById(id); --- IGNORE ---
    }

    @Override
    @CacheEvict(value = "users", key = "#userId")
    public UserResponseDto assignRoles(Integer userId, List<Long> roleIds) {
        User user = repository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        
        List<Role> roles = roleRepository.findAllById(roleIds);
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        
        return mapToResponse(repository.save(user));
    }

    private UserResponseDto mapToResponse(User user) {
        UserResponseDto response = new UserResponseDto();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());
        response.setEmail(user.getEmail());
        response.setPhonenumber(user.getPhonenumber());
        response.setStatus(user.getStatus());
        response.setLastLogin(user.getLastLogin());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .toList());
        }
        
        return response;
    }
}
