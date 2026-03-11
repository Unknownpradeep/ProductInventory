package com.hepl.product.Service;

import java.util.List;


import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;


public interface ProductService {

    List< ProductResponseDto> listAll();

     ProductResponseDto get(Long id);

    //ProductDto save(Long id,ProductDto dto);

    ProductResponseDto update(Long id, ProductRequestDto dto);

    void delete(Long id);

    List< ProductResponseDto> findByCategory(Long categoryId);

    ProductResponseDto save(ProductRequestDto dto);

    List<ProductResponseDto> findByCustomer(Long customerId);

//ProductResponseDto update(Long id, ProductRequestDto dto);
}