package com.hepl.product.Service;

import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.CustomerDto.CustomerRequestDto;
import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;
import com.hepl.product.model.Customer;

public interface CustomerService {
    Page<CustomerResponseDto> listAll(String search, String name, String email, String state, String country, int page, int size, String sortBy, String sortDir);
    CustomerResponseDto get(Long id);
    CustomerResponseDto save(CustomerRequestDto customer);
    CustomerResponseDto update(Long id, Customer customer);
    void delete(Long id);
}
