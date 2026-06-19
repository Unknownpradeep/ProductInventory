package com.hepl.product.Service.ServiceImpl;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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
    private final com.hepl.product.Service.SocketIOService socketIOService;

    @Override
    public Page<StockRespnseDto> listAll(String search, String type, Long productId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return stockRepository.searchAndFilter(search, type, productId, PageRequest.of(page, size, sort)).map(this::mapToDto);
    }

    @Override
    @CacheEvict(value = "stocks", allEntries = true)
    public StockRespnseDto addStock(StockRequestDto dto) {

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setProductName(product.getName());
        stock.setQuantity(dto.getQuantity());
        stock.setType(dto.getType());
        stock.setExpiryDate(dto.getExpiryDate());
        stock.setSaleableStock(dto.getSaleableStock());
        stock.setNonSaleableStock(dto.getNonSaleableStock());
        stock.setCreatedAt(java.time.LocalDate.now());

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

        StockRespnseDto resDto = mapToDto(saved);
        try {
            socketIOService.emitStockUpdated(resDto);
            socketIOService.emitNotification("Stock Updated", "Stock for product " + resDto.getProductName() + " has been updated (" + resDto.getType() + " " + resDto.getQuantity() + ").", "info");
            
            if (product.getQuantity() < 10) {
                socketIOService.emitNotification("Low Stock Alert", "Product " + product.getName() + " is running low on stock! Only " + product.getQuantity() + " remaining.", "warning");
            }
        } catch (Exception e) {
            System.err.println("Failed to emit stock updated socket event: " + e.getMessage());
        }
        return resDto;
    }

    @Override
    @Cacheable(value = "stocks", key = "#productId")
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

    @Override
    @CacheEvict(value = "stocks", allEntries = true)
    public void delete(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock Not Found"));
        stock.setDeleted(true);
        stockRepository.save(stock);
    }

    @Override
    public List<Stock> findAllEntities() {
        return stockRepository.findAll();
    }

    private StockRespnseDto mapToDto(Stock stock) {

        StockRespnseDto dto = new StockRespnseDto();
        dto.setId(stock.getId());
        dto.setProductId(stock.getProduct() != null ? stock.getProduct().getId() : null);
        dto.setProductName(stock.getProduct() != null ? stock.getProduct().getName() : null);
        dto.setQuantity(stock.getQuantity());
        dto.setCreatedAt(stock.getCreatedAt() != null ? stock.getCreatedAt().atStartOfDay() : null);
        dto.setType(stock.getType());
        dto.setExpiryDate(stock.getExpiryDate() != null ? stock.getExpiryDate() : (stock.getProduct() != null ? stock.getProduct().getExpiryDate() : null));
        dto.setSaleableStock(stock.getSaleableStock());
        dto.setNonSaleableStock(stock.getNonSaleableStock());

        return dto;
    }
}