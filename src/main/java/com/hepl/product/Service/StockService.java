package com.hepl.product.Service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.StockDto.StockRequestDto;
import com.hepl.product.Payload.Dto.StockDto.StockRespnseDto;
import com.hepl.product.model.Stock;

public interface StockService {
    StockRespnseDto addStock(StockRequestDto dto);
    StockRespnseDto getAvailableStock(Long productId);
    Page<StockRespnseDto> listAll(String search, String type, Long productId, int page, int size, String sortBy, String sortDir);
    void delete(Long id);
    List<Stock> findAllEntities();
}
