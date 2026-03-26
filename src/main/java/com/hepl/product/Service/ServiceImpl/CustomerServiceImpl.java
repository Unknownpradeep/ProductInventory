package com.hepl.product.Service.ServiceImpl;

import java.util.List;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.CustomerDto.CustomerRequestDto;
import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;
import com.hepl.product.Repository.CustomerRepository;
import com.hepl.product.Service.CustomerService;
import com.hepl.product.model.Customer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository repository;

    @Override
    public List<CustomerResponseDto> listAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public CustomerResponseDto get(Long id) {
        return mapToDto(repository.findById(id).orElse(null));
    }

    @Override
    public CustomerResponseDto save(CustomerRequestDto customer) {
        Customer c = new Customer();
        c.setName(customer.getName());
        c.setEmail(customer.getEmail());
        c.setAddress(customer.getAddress());
       // c.setCity(customer.getCity());
        c.setState(customer.getState());
        c.setPincode(customer.getPincode());
        return mapToDto(repository.save(c));
    }

    @Override
    public CustomerResponseDto update(Long id, Customer customer) {
        Customer existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Customer Not Found"));
        existing.setName(customer.getName());
        existing.setEmail(customer.getEmail());
        existing.setAddress(customer.getAddress());
        existing.setState(customer.getState());
        existing.setCountry(customer.getCountry());
        existing.setPincode(customer.getPincode());

        return mapToDto(repository.save(existing));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }


    private CustomerResponseDto mapToDto(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setAddress(customer.getAddress());
        dto.setPincode(customer.getPincode());
        return dto;
    }
}
