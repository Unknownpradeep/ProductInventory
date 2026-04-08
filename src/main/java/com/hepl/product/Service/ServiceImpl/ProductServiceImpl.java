package com.hepl.product.Service.ServiceImpl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public Page<ProductResponseDto> listAll(String search, String code, Long divisionId, Double minPrice, Double maxPrice, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repository.searchAndFilter(search, code, divisionId, minPrice, maxPrice, pageable).map(this::mapToDto);
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
        String divisionName = dto.getDivisionName() != null ? dto.getDivisionName().trim() : null;

       if (divisionName == null || divisionName.isBlank()) {
        throw new RuntimeException("Division name is required");
       }
        // if (dto.getDivisionName() == null || dto.getDivisionName().isBlank()) {
        //   throw new RuntimeException("Division name is required");
        // }

        Division division = divisionRepository
          .findByNameIgnoreCase(divisionName)
          .orElseThrow(() -> new RuntimeException("Division not found"));
        p.setDivision(division);
        p.setDivisionName(division.getName());
            // .orElseThrow(() -> new RuntimeException("Division not found"));
            // p.setDivision(division);
        
  
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
        p.setSku(dto.getSku());
        p.setUom(dto.getUom());
        p.setExpiryDate(dto.getExpiryDate());
        p.setSaleableStock(dto.getSaleableStock());
        p.setNonSaleableStock(dto.getNonSaleableStock());
         String divisionName = dto.getDivisionName() != null ? dto.getDivisionName().trim() : null;

    if (divisionName == null || divisionName.isBlank()) {
        throw new RuntimeException("Division name is required");
    }

    Division division = divisionRepository
            .findByNameIgnoreCase(divisionName)
            .orElseThrow(() -> new RuntimeException("Division not found"));

    p.setDivision(division);
    p.setDivisionName(divisionName);
    System.out.println("Before save divisionName: " + p.getDivisionName());
      

        Product updated = repository.save(p);
        return mapToDto(updated);
    }

    @Override
    public void delete(Long id) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));
        existing.setDeleted(true);
        repository.save(existing);
     //   repository.deleteById(id); --- IGNORE ---
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
        // dto.setSaleableStock(product.getSaleableStock());
        // dto.setNonSaleableStock(product.getNonSaleableStock());
        // dto.setSku(product.getSku());
        // dto.setUom(product.getUom());
        dto.setDivisionName(
         product.getDivision() != null 
          ? product.getDivision().getName() 
          : null);
        return dto;
    }
}

