package com.hepl.product.Service;

import java.util.List;

import com.hepl.product.Payload.Dto.CustomerDto.CustomerRequestDto;
import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;
import com.hepl.product.model.Customer;

public interface CustomerService {
    List<CustomerResponseDto> listAll();
    CustomerResponseDto get(Long id);
    CustomerResponseDto save(CustomerRequestDto customer);
    CustomerResponseDto update(Long id, Customer customer);
    void delete(Long id);
}
