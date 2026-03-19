package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;
import com.hepl.product.Payload.Dto.OrderDto.OrderItemDto;
import com.hepl.product.Payload.Dto.OrderDto.OrderItemResponseDto;
import com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto;
import com.hepl.product.Payload.Dto.OrderDto.OrderResponseDto;
import com.hepl.product.Repository.CustomerRepository;
import com.hepl.product.Repository.OrderItemRepository;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Service.OrderService;
import com.hepl.product.model.Customer;
import com.hepl.product.model.Order;
import com.hepl.product.model.OrderItem;
import com.hepl.product.model.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    public List<OrderResponseDto> listAll() {
        return orderRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public OrderResponseDto get(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        return mapToDto(order);
    }

    @Override
    public OrderResponseDto save(OrderRequestDto dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer Not Found"));

        Order order = new Order();
        order.setOrderCode("ORD" + System.currentTimeMillis());
        order.setCustomer(customer);
        order.setCustomerName(customer.getName());
        order.setCustomerEmail(customer.getEmail());
        order.setShippingAddress(dto.getShippingAddress());
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setOrderDate(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = new ArrayList<>();
        double totalAmount = 0;

        for (OrderItemDto itemDto : dto.getOrderItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product Not Found: " + itemDto.getProductId()));

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            item.setCategoryName(product.getCategory());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(product.getPrice());
            item.setTotalPrice(product.getPrice() * itemDto.getQuantity());

            items.add(item);
            totalAmount += item.getTotalPrice();
        }

        orderItemRepository.saveAll(items);
        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        return mapToDto(savedOrder);
    }

    @Override
    public OrderResponseDto update(Long id, OrderRequestDto dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer Not Found"));

        order.setCustomer(customer);
        order.setCustomerName(customer.getName());
        order.setCustomerEmail(customer.getEmail());
        order.setShippingAddress(dto.getShippingAddress());
        order.setUpdatedAt(LocalDateTime.now());

        orderItemRepository.deleteAll(orderItemRepository.findByOrderId(order.getId()));

        List<OrderItem> items = new ArrayList<>();
        double totalAmount = 0;

        for (OrderItemDto itemDto : dto.getOrderItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product Not Found: " + itemDto.getProductId()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            item.setCategoryName(product.getCategory());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(product.getPrice());
            item.setTotalPrice(product.getPrice() * itemDto.getQuantity());

            items.add(item);
            totalAmount += item.getTotalPrice();
        }

        orderItemRepository.saveAll(items);
        order.setTotalAmount(totalAmount);

        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto updatePaymentStatus(Long id, String paymentStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        order.setPaymentStatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return mapToDto(orderRepository.save(order));
    }

    @Override
    public void delete(Long id) {
        orderItemRepository.deleteAll(orderItemRepository.findByOrderId(id));
        orderRepository.deleteById(id);
    }

    @Override
    public List<OrderResponseDto> findByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(this::mapToDto).toList();
    }

    @Override
    public List<OrderResponseDto> findByStatus(String status) {
        return orderRepository.findByStatus(status).stream().map(this::mapToDto).toList();
    }

    private OrderResponseDto mapToDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTotalAmount(order.getTotalAmount());

        if (order.getCustomer() != null) {
            CustomerResponseDto customerDto = new CustomerResponseDto();
            customerDto.setId(order.getCustomer().getId());
            customerDto.setName(order.getCustomer().getName());
            customerDto.setEmail(order.getCustomer().getEmail());
            dto.setCustomer(customerDto);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponseDto> productDtos = items.stream().map(item -> {
            OrderItemResponseDto itemDto = new OrderItemResponseDto();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setProductName(item.getProductName() != null ? item.getProductName() : item.getProduct().getName());
            itemDto.setPrice(item.getPrice());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setTotalPrice(item.getTotalPrice());
            itemDto.setCategory(item.getCategoryName() != null ? item.getCategoryName() : item.getProduct().getCategory());
            return itemDto;
        }).toList();

        dto.setProducts(productDtos);
        return dto;
    }
}
