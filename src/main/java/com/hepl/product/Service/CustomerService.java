package com.hepl.product.Service;

import java.util.List;
import com.hepl.product.model.Customer;

public interface CustomerService {
    List<Customer> listAll();
    Customer get(Long id);
    Customer save(Customer customer);
    Customer update(Long id, Customer customer);
    void delete(Long id);
}
