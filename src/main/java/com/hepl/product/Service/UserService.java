package com.hepl.product.Service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.UserDto.UserRequestDto;
import com.hepl.product.Payload.Dto.UserDto.UserResponseDto;

public interface UserService {
    Page<UserResponseDto> listAll(String search, String username, String email, String status, int page, int size, String sortBy, String sortDir);
    UserResponseDto get(Integer id);
    UserResponseDto save(UserRequestDto dto);
    UserResponseDto update(Integer id, UserRequestDto dto);
    void delete(Integer id);
    UserResponseDto assignRoles(Integer userId, List<Long> roleIds);
}
