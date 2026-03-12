package com.hepl.product.Service.ServiceImpl;

import java.util.List;


import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;
import com.hepl.product.Repository.CategoryRepository;
import com.hepl.product.Repository.CustomerRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Service.ProductService;
import com.hepl.product.model.Category;
import com.hepl.product.model.Customer;
import com.hepl.product.model.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;

    @Override
    public List<ProductResponseDto> listAll() {

        List<Product> p = repository.findAll(Sort.by(Sort.Direction.ASC,"price"));

        return p.stream().map(this::mapToDto).toList();
    }

    @Override
    public ProductResponseDto get(Long id) {

        Product p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        return mapToDto(p);
    }

    @Override
public ProductResponseDto save(ProductRequestDto dto) {

    Product p = new Product();

    p.setName(dto.getName());
    p.setPrice(dto.getPrice());
    p.setQuantity(dto.getQuantity());
    p.setCode("P" + System.currentTimeMillis());

    // Find Category using id
    Category categoryEntity = categoryRepository
            .findById(dto.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category Not Found"));

    // Find Customer using id
    Customer customerEntity = customerRepository
            .findById(dto.getCustomerId())
            .orElseThrow(() -> new RuntimeException("Customer Not Found"));

    p.setCategoryObj(categoryEntity);
    p.setCategory(categoryEntity.getName());
    
    p.setCustomerObj(customerEntity);
    p.setCustomer(customerEntity.getName());

    Product saved = repository.save(p);

    return mapToDto(saved);
}

   @Override
public ProductResponseDto update(Long id, ProductRequestDto dto) {

    Product p = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product Not Found"));

    p.setName(dto.getName());

    Category categoryEntity = categoryRepository
            .findById(dto.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category Not Found"));

    Customer customerEntity = customerRepository
            .findById(dto.getCustomerId())
            .orElseThrow(() -> new RuntimeException("Customer Not Found"));

    p.setCategoryObj(categoryEntity);
    p.setCategory(categoryEntity.getName());
    
    p.setCustomerObj(customerEntity);
    p.setCustomer(customerEntity.getName());

    p.setPrice(dto.getPrice());
    p.setQuantity(dto.getQuantity());

    Product updated = repository.save(p);

    return mapToDto(updated);
}

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<ProductResponseDto> findByCategory(Long categoryId) {

        List<Product> p = repository.findByCategoryObjId(categoryId);

        return p.stream().map(this::mapToDto).toList();
    }
    @Override
    public List<ProductResponseDto> findByCustomer(Long customerId) {

    List<Product> products = repository.findByCustomerObjId(customerId);

    return products.stream().map(this::mapToDto).toList();
}

    private ProductResponseDto mapToDto(Product product){

    ProductResponseDto dto = new ProductResponseDto();

    dto.setName(product.getName());
    dto.setPrice(product.getPrice());
    dto.setQuantity(product.getQuantity());

    if(product.getCategoryObj()!=null){
        dto.setCategoryName(product.getCategoryObj().getName());
        dto.setCategoryId(product.getCategoryObj().getId());
    }

    if(product.getCustomerObj()!=null){
        dto.setCustomerName(product.getCustomerObj().getName());
        dto.setCustomerId(product.getCustomerObj().getId());
    }

    return dto;
}

}