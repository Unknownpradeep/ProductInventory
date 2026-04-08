package com.hepl.product.Service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto;
import com.hepl.product.Payload.Dto.OrderDto.OrderResponseDto;

public interface OrderService {
    Page<OrderResponseDto> listAll(String search, String status, String paymentStatus, Long customerId, int page, int size, String sortBy, String sortDir);
    OrderResponseDto get(Long id);
    OrderResponseDto save(OrderRequestDto dto);
    OrderResponseDto update(Long id, OrderRequestDto dto);
    OrderResponseDto updateStatus(Long id, String status);
    OrderResponseDto updatePaymentStatus(Long id, String paymentStatus);
    void delete(Long id);
    List<OrderResponseDto> findByCustomer(Long customerId);
    List<OrderResponseDto> findByStatus(String status);
    List<OrderResponseDto> saveMultiple(List<OrderRequestDto> orders);
    OrderResponseDto getByCode(String orderCode);
}
