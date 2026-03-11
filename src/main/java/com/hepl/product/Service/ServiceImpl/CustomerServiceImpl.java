package com.hepl.product.Service.ServiceImpl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.hepl.product.Repository.CustomerRepository;
import com.hepl.product.Service.CustomerService;
import com.hepl.product.model.Customer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository repository;

    @Override
    public List<Customer> listAll() {
        return repository.findAll();
    }

    @Override
    public Customer get(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Customer save(Customer customer) {
        return repository.save(customer);
    }

    @Override
    public Customer update(Long id, Customer customer) {
        Customer existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Customer Not Found"));
        existing.setName(customer.getName());
        existing.setEmail(customer.getEmail());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
