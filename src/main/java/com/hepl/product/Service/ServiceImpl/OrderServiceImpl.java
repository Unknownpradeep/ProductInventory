package com.hepl.product.Service.ServiceImpl;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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
import com.hepl.product.Service.StockService;
import com.hepl.product.Util.QrGenerator;
import com.hepl.product.model.Customer;
import com.hepl.product.model.Order;
import com.hepl.product.model.OrderItem;
import com.hepl.product.model.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final com.hepl.product.Service.SocketIOService socketIOService;
    private final com.hepl.product.Service.EmailService emailService;

    public Page<OrderResponseDto> listAll(String search, String status, String paymentStatus, Long customerId, LocalDateTime startDate, LocalDateTime endDate, String preset, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        LocalDateTime finalStartDate = startDate;
        LocalDateTime finalEndDate = endDate;

        if (preset != null && !preset.isBlank()) {
            finalEndDate = LocalDateTime.now();
            switch (preset.toLowerCase()) {
                case "day":
                    finalStartDate = LocalDateTime.now().minusDays(1);
                    break;
                case "week":
                    finalStartDate = LocalDateTime.now().minusWeeks(1);
                    break;
                case "month":
                    finalStartDate = LocalDateTime.now().minusMonths(1);
                    break;
                default:
                    break;
            }
        }

        Page<Order> ordersPage = orderRepository.searchAndFilter(search, status, paymentStatus, customerId, finalStartDate, finalEndDate, pageable);
        return ordersPage.map(this::mapToDto);
    }

    @Override
    public List<OrderResponseDto> saveMultiple(List<OrderRequestDto> orders) {

        List<OrderResponseDto> responseList = new ArrayList<>();

        for (OrderRequestDto dto : orders) {
            responseList.add(save(dto));
        }

        return responseList;
    }

    @Override
    @Cacheable(value = "orders", key = "#id")
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
        order.setOrderCode("ORD" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setCustomer(customer);
        order.setCustomerName(customer.getName());
        order.setCustomerEmail(customer.getEmail());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            order.setStatus(dto.getStatus().toUpperCase());
        } else {
            order.setStatus("CREATED");
        }
        if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isBlank()) {
            order.setPaymentstatus(dto.getPaymentStatus().toUpperCase());
        } else {
            order.setPaymentstatus("PENDING");
        }
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setOnlinePaymentOption(dto.getOnlinePaymentOption());

        order.setShippingAddress(customer.getAddress());

        order.setOrderDate(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        List<OrderItem> items = new ArrayList<>();
        double baseTotal = 0;
        double totalDiscount = 0;
        double totalTax = 0;
        double finalTotal = 0;

        for (OrderItemDto itemDto : dto.getOrderItems()) {

            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product Not Found: " + itemDto.getProductId()));

            if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(java.time.LocalDate.now())) {
                throw new RuntimeException("Product is expired: " + product.getName() + " (Expired on " + product.getExpiryDate() + ")");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);

            double price = product.getPrice();
            int qty = itemDto.getQuantity();

            double base = Math.round(price * qty);
            baseTotal += base;

            double discountAmt = Math.round((base * itemDto.getDiscount()) / 100);
            totalDiscount += discountAmt;

            double afterDiscount = base - discountAmt;

            double tax = Math.round((afterDiscount * itemDto.getGstpercentage()) / 100);
            totalTax += tax;

            double finalAmount = Math.round(afterDiscount + tax);

            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            item.setDivisionName(product.getDivision() != null ? product.getDivision().getName() : null);
            item.setQuantity(qty);
            item.setPrice(price);
            item.setDiscount(itemDto.getDiscount());
            item.setGstpercentage(itemDto.getGstpercentage());
            item.setTaxamount(tax);
            item.setTotalPrice(finalAmount);

            items.add(item);

            finalTotal += finalAmount;
        }

        order.setTotalPrice(finalTotal);

        order.setBaseTotal(baseTotal);
        order.setTotalDiscount(totalDiscount);
        order.setTotalTax(totalTax);

        order.setOrderItems(items);

        Order savedOrder = orderRepository.save(order);

        String qrText = baseUrl + "/api/v1/orders/code/" + savedOrder.getOrderCode();

        String folderPath = "qr-codes/";
        String filePath = folderPath + savedOrder.getOrderCode() + ".png";

        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        try {

            QrGenerator.generateQrCode(qrText, filePath);

            savedOrder.setQrCodePath(filePath);

        } catch (Exception e) {
            System.out.println("QR generation failed, but order saved");
        }

        savedOrder = orderRepository.save(savedOrder);
        OrderResponseDto responseDto = mapToDto(savedOrder);
        try {
            socketIOService.emitOrderCreated(responseDto);
            socketIOService.emitNotification("New Order Created", "Order " + savedOrder.getOrderCode() + " has been placed successfully.", "success");
        } catch (Exception e) {
            System.err.println("Failed to emit order created socket event: " + e.getMessage());
        }

        // Send order confirmation email via Mailtrap
        try {
            emailService.sendOrderCreatedEmail(savedOrder);
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }

        return responseDto;
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponseDto update(Long id, OrderRequestDto dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer Not Found"));

        order.setCustomer(customer);
        order.setCustomerName(customer.getName());
        order.setCustomerEmail(customer.getEmail());

        order.setUpdatedAt(LocalDateTime.now());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            order.setStatus(dto.getStatus());
        }
        if (dto.getPaymentStatus() != null && !dto.getPaymentStatus().isBlank()) {
            order.setPaymentstatus(dto.getPaymentStatus());
        }
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setOnlinePaymentOption(dto.getOnlinePaymentOption());

        orderItemRepository.deleteAll(orderItemRepository.findByOrderId(order.getId()));

        List<OrderItem> items = new ArrayList<>();
        double baseTotal = 0;
        double totalDiscount = 0;
        double totalTax = 0;
        double finalTotal = 0;

        for (OrderItemDto itemDto : dto.getOrderItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product Not Found: " + itemDto.getProductId()));

            if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(java.time.LocalDate.now())) {
                throw new RuntimeException("Product is expired: " + product.getName() + " (Expired on " + product.getExpiryDate() + ")");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            item.setDivisionName(product.getDivision() != null ? product.getDivision().getName() : null);
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(product.getPrice());

            double base = Math.round(product.getPrice() * itemDto.getQuantity());
            double discountAmt = Math.round((base * itemDto.getDiscount()) / 100);
            double afterDiscount = base - discountAmt;
            double tax = Math.round((afterDiscount * itemDto.getGstpercentage()) / 100);
            double lineTotal = Math.round(afterDiscount + tax);

            item.setDiscount(itemDto.getDiscount());
            item.setGstpercentage(itemDto.getGstpercentage());
            item.setTaxamount(tax);
            item.setTotalPrice(lineTotal);

            items.add(item);
            baseTotal += base;
            totalDiscount += discountAmt;
            totalTax += tax;
            finalTotal += lineTotal;
        }

        orderItemRepository.saveAll(items);
        order.setBaseTotal(baseTotal);
        order.setTotalDiscount(totalDiscount);
        order.setTotalTax(totalTax);
        order.setTotalPrice(finalTotal);

        Order savedOrder = orderRepository.save(order);
        OrderResponseDto responseDto = mapToDto(savedOrder);
        try {
            socketIOService.emitOrderStatusUpdate(savedOrder.getOrderCode(), savedOrder.getStatus());
            socketIOService.emitOrderPaymentUpdate(savedOrder.getOrderCode(), savedOrder.getPaymentstatus());
            socketIOService.emitNotification("Order Updated", "Order " + savedOrder.getOrderCode() + " details or status updated.", "info");
        } catch (Exception e) {
            System.err.println("Failed to emit order updated socket event: " + e.getMessage());
        }
        return responseDto;
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponseDto updateStatus(Long id, String status) {
        return updateStatus(id, status, null);
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponseDto updateStatus(Long id, String status, String remark) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        String currentStatus = order.getStatus();

        // Prevent changing status of delivered order unless cancelling/returning
        if ("DELIVERED".equalsIgnoreCase(currentStatus) && !"CANCELLED".equalsIgnoreCase(status)) {
            throw new RuntimeException("Cannot change status of a delivered order");
        }

        // Enforce 10-day return limit and cancellation before packing stage
        if ("CANCELLED".equalsIgnoreCase(status)) {
            if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
                if (order.getOrderDate() != null) {
                    java.time.LocalDateTime limitDate = order.getOrderDate().plusDays(10);
                    if (java.time.LocalDateTime.now().isAfter(limitDate)) {
                        throw new RuntimeException("Return period expired. Returns are only permitted within 10 days of order placement.");
                    }
                }
            }
            order.setCancellationRemarks(remark);
        }

        // Restore stock if transitioning to CANCELLED
        if ("CANCELLED".equalsIgnoreCase(status)) {
            if (java.util.List.of("CONFIRMED", "STOCK_RESERVED", "READY_FOR_DISPATCH", "PARTIALLY_DISPATCHED", "DISPATCHED", "OUT_FOR_DELIVERY", "DELIVERED").contains(currentStatus.toUpperCase())) {
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                for (OrderItem item : items) {
                    Product product = item.getProduct();
                    product.setQuantity(product.getQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        // Check stock when confirming
        if ("PENDING".equalsIgnoreCase(currentStatus) && "CONFIRMED".equalsIgnoreCase(status)) {
            for (OrderItem item : order.getOrderItems()) {
                int available = stockService.getAvailableStock(item.getProduct().getId()).getQuantity();
                if (available < item.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + item.getProduct().getName());
                }
            }
        }

        // Deduct product quantity when order is DELIVERED
        if ("DELIVERED".equalsIgnoreCase(status)) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                Product product = item.getProduct();
                int newQty = product.getQuantity() - item.getQuantity();
                if (newQty < 0)
                    newQty = 0;
                product.setQuantity(newQty);
                productRepository.save(product);
            }
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        try {
            socketIOService.emitOrderStatusUpdate(savedOrder.getOrderCode(), savedOrder.getStatus());
            socketIOService.emitNotification("Order Status Updated", "Order " + savedOrder.getOrderCode() + " is now " + savedOrder.getStatus() + ".", "info");
        } catch (Exception e) {
            System.err.println("Failed to emit order status update socket event: " + e.getMessage());
        }
        return mapToDto(savedOrder);
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponseDto updatePaymentStatus(Long id, String paymentStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        order.setPaymentstatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        try {
            socketIOService.emitOrderPaymentUpdate(savedOrder.getOrderCode(), savedOrder.getPaymentstatus());
            socketIOService.emitNotification("Payment Status Updated", "Order " + savedOrder.getOrderCode() + " payment is now " + savedOrder.getPaymentstatus() + ".", "info");
        } catch (Exception e) {
            System.err.println("Failed to emit order payment update socket event: " + e.getMessage());
        }
        return mapToDto(savedOrder);
    }

    @Override
    @Cacheable(value = "orders", key = "#orderCode")
    public OrderResponseDto getByCode(String orderCode) {
        return mapToDto(orderRepository.findByOrderCode(orderCode));
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public void delete(Long id) {
        Order existing = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order Not Found"));
        existing.setDeleted(true);
        orderRepository.save(existing);
    }

    @Override
    public List<Order> findAllEntities() {
        return orderRepository.findAll();
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
        dto.setFinalAmount(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentstatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setOnlinePaymentOption(order.getOnlinePaymentOption());
        dto.setCancellationRemarks(order.getCancellationRemarks());
        Double baseTotal = order.getBaseTotal();
        Double discount = order.getTotalDiscount();
        Double tax = order.getTotalTax();

        dto.setBaseTotal(baseTotal == null ? 0.0 : baseTotal);
        dto.setTotalDiscount(discount == null ? 0.0 : discount);
        dto.setTotalTax(tax == null ? 0.0 : tax);

        if (order.getCustomer() != null) {
            CustomerResponseDto customerDto = new CustomerResponseDto();
            customerDto.setId(order.getCustomer().getId());

            customerDto.setName(order.getCustomer().getName());
            customerDto.setEmail(order.getCustomer().getEmail());
            customerDto.setAddress(order.getCustomer().getAddress());
            customerDto.setState(order.getCustomer().getState());
            customerDto.setCountry(order.getCustomer().getCountry());
            customerDto.setPincode(order.getCustomer().getPincode());

            dto.setCustomer(List.of(customerDto));
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponseDto> productDtos = items.stream().map(item -> {
            OrderItemResponseDto itemDto = new OrderItemResponseDto();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setProductName(item.getProductName() != null ? item.getProductName() : item.getProduct().getName());
            itemDto.setProductCode(item.getProductCode());
            itemDto.setDivisionName(item.getDivisionName() != null ? item.getDivisionName() :
                    (item.getProduct().getDivision() != null ? item.getProduct().getDivision().getName() : null));
            itemDto.setQuantity(item.getQuantity());
            itemDto.setDispatchedQuantity(item.getDispatchedQuantity());
            itemDto.setPrice(item.getPrice());
            itemDto.setDiscount(item.getDiscount());
            itemDto.setTaxamount(item.getTaxamount());
            itemDto.setGstpercentage(item.getGstpercentage());

            itemDto.setTotalPrice(item.getTotalPrice());
            itemDto.setStatus(item.getStatus());
            itemDto.setBatchcode(item.getProduct() != null ? item.getProduct().getBatchcode() : "");

            return itemDto;
        }).toList();

        dto.setProducts(productDtos);
        return dto;
    }

}
