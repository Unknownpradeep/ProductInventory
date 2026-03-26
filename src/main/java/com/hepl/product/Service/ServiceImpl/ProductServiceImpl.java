package com.hepl.product.Service.ServiceImpl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;
import com.hepl.product.Repository.DivisionRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Service.ProductService;
import com.hepl.product.model.Division;
import com.hepl.product.model.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final DivisionRepository divisionRepository;

    @Override
    public List<ProductResponseDto> listAll() {
        List<Product> p = repository.findAll(Sort.by(Sort.Direction.ASC, "price"));
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
        p.setSku(dto.getSku());
        p.setUom(dto.getUom());
        p.setExpiryDate(dto.getExpiryDate());
        p.setSaleableStock(dto.getSaleableStock());
        p.setNonSaleableStock(dto.getNonSaleableStock());
         Division division = divisionRepository.findById(dto.getDivisionId())
            .orElseThrow(() -> new RuntimeException("Division not found"));
            p.setDivision(division);

        Product saved = repository.save(p);
        return mapToDto(saved);
    }

    @Override
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        p.setName(dto.getName());
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
        List<Product> p = repository.findByDivisionId(categoryId);
        return p.stream().map(this::mapToDto).toList();
    }

    @Override
    public List<ProductResponseDto> findByCustomer(Long customerId) {
        return List.of();
    }

    private ProductResponseDto mapToDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        dto.setExpiryDate(product.getExpiryDate());
        return dto;
    }
}
