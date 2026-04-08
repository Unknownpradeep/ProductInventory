package com.hepl.product.Service.ServiceImpl;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    


    public Page<OrderResponseDto> listAll(String search, String status, String paymentStatus, Long customerId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> ordersPage = orderRepository.searchAndFilter(search, status, paymentStatus, customerId, pageable);
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
    order.setStatus("PENDING");
    order.setPaymentstatus("PENDING");

   
      order.setShippingAddress(customer.getAddress());


    order.setOrderDate(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
   
    List<OrderItem> items = new ArrayList<>();
    double baseTotal =  0;
    double totalDiscount = 0;
    double totalTax = 0;
    double finalTotal = 0;

    for (OrderItemDto itemDto : dto.getOrderItems()) {

        Product product = productRepository.findById(itemDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product Not Found: " + itemDto.getProductId()));

        OrderItem item = new OrderItem();
        item.setOrder(order);  

        double price = product.getPrice();
        int qty = itemDto.getQuantity();

        double base =Math.round(price * qty) ;
        baseTotal += base;

        double discountAmt = Math.round((base * itemDto.getDiscount()) / 100);
        totalDiscount += discountAmt;

        double afterDiscount = base - discountAmt;

        double tax =Math.round((afterDiscount * itemDto.getGstpercentage()) / 100);
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

    order.setTotalPrice( finalTotal);

    order.setBaseTotal(baseTotal);
    order.setTotalDiscount(totalDiscount);
    order.setTotalTax(totalTax);


    order.setOrderItems(items);
   

    
    Order savedOrder = orderRepository.save(order);

  String qrText = "http://localhost:8080/api/v1/orders/code/" + savedOrder.getOrderCode();


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


    savedOrder=orderRepository.save(savedOrder);
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
            item.setDivisionName(product.getDivision() != null ? product.getDivision().getName() : null);
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(product.getPrice());
            item.setTotalPrice(product.getPrice() * itemDto.getQuantity());
          

            items.add(item);
            totalAmount += item.getTotalPrice();
        }

        orderItemRepository.saveAll(items);
        order.setTotalPrice(totalAmount);

        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        String currentStatus = order.getStatus();

        // Prevent changing status of delivered order
        if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
            throw new RuntimeException("Cannot change status of a delivered order");
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
                if (newQty < 0) newQty = 0;
                product.setQuantity(newQty);
                productRepository.save(product);
            }
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto updatePaymentStatus(Long id, String paymentStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        order.setPaymentstatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return mapToDto(orderRepository.save(order));
    }
    @Override
         public OrderResponseDto getByCode(String orderCode) {
             return mapToDto(orderRepository.findByOrderCode(orderCode));
         }

    @Override
    public void delete(Long id) {
        Order existing=orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order Not Found"));
        existing.setDeleted(true);
        orderRepository.save(existing);
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
        //dto.setSubTotal(order.getSubTotal());
       // dto.setTotalDiscount(order.getTotalDiscount());
       // dto.setTotalTax(order.getTotalTax());
        dto.setFinalAmount(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentstatus());
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
            itemDto.setPrice(item.getPrice());
            itemDto.setDiscount(item.getDiscount());
            itemDto.setTaxamount(item.getTaxamount());
            itemDto.setGstpercentage(item.getGstpercentage());

           
            itemDto.setTotalPrice(item.getTotalPrice());
            itemDto.setStatus(item.getStatus());
            
            return itemDto;
        }).toList();



        dto.setProducts(productDtos);
        return dto;
    }
 
}
