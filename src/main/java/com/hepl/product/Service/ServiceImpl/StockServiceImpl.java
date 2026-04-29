package com.hepl.product.Service.ServiceImpl;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.StockDto.StockRequestDto;
import com.hepl.product.Payload.Dto.StockDto.StockRespnseDto;
import com.hepl.product.Repository.OrderItemRepository;

import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Repository.StockRepository;
import com.hepl.product.Service.StockService;
import com.hepl.product.model.Product;
import com.hepl.product.model.Stock;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Page<StockRespnseDto> listAll(String search, String type, Long productId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return stockRepository.searchAndFilter(search, type, productId, PageRequest.of(page, size, sort)).map(this::mapToDto);
    }

    @Override
    public StockRespnseDto addStock(StockRequestDto dto) {

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setProductName(product.getName());
        stock.setQuantity(dto.getQuantity());
        stock.setType(dto.getType());

        Stock saved = stockRepository.save(stock);

        // Update product quantity based on stock type
        if ("IN".equalsIgnoreCase(dto.getType())) {
            product.setQuantity(product.getQuantity() + dto.getQuantity());
        } else if ("OUT".equalsIgnoreCase(dto.getType())) {
            int newQty = product.getQuantity() - dto.getQuantity();
            if (newQty < 0) throw new RuntimeException("Insufficient stock for product: " + product.getName());
            product.setQuantity(newQty);
        }
        productRepository.save(product);

        return mapToDto(saved);
    }

    @Override
    public StockRespnseDto getAvailableStock(Long productId) {

        Product  product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));
        //String productName = product.getName();

        int productQuantity = product.getQuantity();
        int orderQuantity=orderItemRepository.getTotalQuantityByProductId(productId);
        int availableStock = productQuantity - orderQuantity;
        
        if (availableStock < 0) {
                availableStock = 0;
            }

        // int total = stockRepository.findByProductId(productId)
        //         .stream()
        //         .mapToInt(s -> "IN".equalsIgnoreCase(s.getType())
        //                 ? s.getQuantity()
        //                 : -s.getQuantity())
        //         .sum();

        StockRespnseDto dto = new StockRespnseDto();
        dto.setProductId(productId);
        dto.setProductName(product.getName());
        
        dto.setQuantity(availableStock);
        dto.setType("AVAILABLE");

        return dto;
    }

    private StockRespnseDto mapToDto(Stock stock) {

        StockRespnseDto dto = new StockRespnseDto();
        dto.setId(stock.getId());
        dto.setProductId(stock.getProduct().getId());
        dto.setProductName(
        stock.getProduct() != null ? stock.getProduct().getName() : null); 
        dto.setQuantity(stock.getQuantity());
        dto.setCreatedAt(stock.getCreatedAt());
        dto.setType(stock.getType());

        return dto;
    }
}