package com.hepl.product.Service.ServiceImpl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public Page<CustomerResponseDto> listAll(String search, String name, String email, String state, String country, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return repository.searchAndFilter(search, name, email, state, country, pageable).map(this::mapToDto);
    }

    @Override
    public CustomerResponseDto get(Long id) {
        return mapToDto(repository.findById(id).orElseThrow(() -> new RuntimeException("Customer Not Found")));
    }

    @Override
    public CustomerResponseDto save(CustomerRequestDto customer) {
        Customer c = new Customer();
        c.setName(customer.getName());
        c.setEmail(customer.getEmail());
        c.setAddress(customer.getAddress());
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
        Customer existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Customer Not Found"));
        existing.setDeleted(true);
        repository.save(existing);
    }

    private CustomerResponseDto mapToDto(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setAddress(customer.getAddress());
        dto.setState(customer.getState());
        dto.setCountry(customer.getCountry());
        dto.setPincode(customer.getPincode());
        return dto;
    }
}
