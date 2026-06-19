package com.hepl.product.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Repository.OrderStatusHistoryRepository;
import com.hepl.product.Repository.DispatchDetailsRepository;
import com.hepl.product.Repository.DeliveryDetailsRepository;
import com.hepl.product.Repository.OrderItemRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.model.Order;
import com.hepl.product.model.OrderStatusHistory;
import com.hepl.product.model.DispatchDetails;
import com.hepl.product.model.DeliveryDetails;
import com.hepl.product.model.OrderItem;
import com.hepl.product.model.Product;
import com.hepl.product.Util.JwtUtil;
import com.hepl.product.Service.SocketIOService;
import com.hepl.product.Service.EmailService;
import com.hepl.product.Repository.WarehouseRepository;
import com.hepl.product.Repository.WarehouseStockRepository;
import com.hepl.product.model.Warehouse;
import com.hepl.product.model.WarehouseStock;

@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;


    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SocketIOService socketIOService;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private DispatchDetailsRepository dispatchDetailsRepository;

    @Autowired
    private DeliveryDetailsRepository deliveryDetailsRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @GetMapping("/orders/{orderId}/history")
    public ResponseEntity<ApiResponse> getOrderHistory(@PathVariable Long orderId) {
        List<OrderStatusHistory> historyList = orderStatusHistoryRepository.findByOrderIdOrderByUpdatedAtAsc(orderId);
        
        List<Map<String, Object>> formattedHistory = historyList.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("status", h.getNewStatus());
            map.put("performedBy", h.getUpdatedBy() != null ? h.getUpdatedBy() : "System");
            map.put("timestamp", h.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", formattedHistory));
    }

    @GetMapping("/orders/{orderId}/dispatch")
    public ResponseEntity<ApiResponse> getDispatchDetails(@PathVariable Long orderId) {
        DispatchDetails details = dispatchDetailsRepository.findByOrderId(orderId).orElse(null);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", details));
    }

    @GetMapping("/orders/{orderId}/delivery")
    public ResponseEntity<ApiResponse> getDeliveryDetails(@PathVariable Long orderId) {
        DeliveryDetails details = deliveryDetailsRepository.findByOrderId(orderId).orElse(null);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", details));
    }

    @PostMapping("/orders/{orderId}/transition")
    public ResponseEntity<ApiResponse> transitionOrder(
            @PathVariable Long orderId,
            @RequestParam String nextStatus,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, Object> payload) {
        
        // Extract role from token
        String token = authHeader.replace("Bearer ", "");
        String userRole = jwtUtil.extractRole(token);
        if (userRole != null) {
            userRole = userRole.toUpperCase();
        } else {
            userRole = "";
        }

        // Validate role access
        boolean isAuthorized = false;
        if (userRole.contains("ADMIN") || userRole.contains("MANAGER")) {
            isAuthorized = true;
        } else if (nextStatus.equalsIgnoreCase("PACK") && (userRole.contains("STORES") || userRole.contains("DISPATCH"))) {
            isAuthorized = true;
        } else if (nextStatus.equalsIgnoreCase("DISPATCH") && userRole.contains("DISPATCH")) {
            isAuthorized = true;
        } else if ((nextStatus.equalsIgnoreCase("OUT_FOR_DELIVERY") || nextStatus.equalsIgnoreCase("DELIVER")) && userRole.contains("DELIVERY")) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(HttpStatus.FORBIDDEN.value(), "Role " + userRole + " is not authorized to transition status to " + nextStatus, null));
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String oldStatus = order.getStatus();

        if ("CANCELLED".equalsIgnoreCase(oldStatus)) {
            throw new RuntimeException("Cannot perform workflow operations on a cancelled or returned order.");
        }

        // Validate workflow sequence and state transitions
        if (nextStatus.equalsIgnoreCase("PACK")) {
            if (!"PENDING".equalsIgnoreCase(oldStatus) && !"CREATED".equalsIgnoreCase(oldStatus)) {
                throw new RuntimeException("Order must be in PENDING state to be packed.");
            }
        } else if (nextStatus.equalsIgnoreCase("DISPATCH")) {
            if (!"READY_FOR_DISPATCH".equalsIgnoreCase(oldStatus) && !"PARTIALLY_DISPATCHED".equalsIgnoreCase(oldStatus)) {
                throw new RuntimeException("Order must be in READY_FOR_DISPATCH or PARTIALLY_DISPATCHED state to be dispatched.");
            }
        } else if (nextStatus.equalsIgnoreCase("OUT_FOR_DELIVERY")) {
            if (!"DISPATCHED".equalsIgnoreCase(oldStatus) && !"PARTIALLY_DISPATCHED".equalsIgnoreCase(oldStatus)) {
                throw new RuntimeException("Order must be in DISPATCHED or PARTIALLY_DISPATCHED state to be set OUT_FOR_DELIVERY.");
            }
        } else if (nextStatus.equalsIgnoreCase("DELIVER")) {
            if (!"OUT_FOR_DELIVERY".equalsIgnoreCase(oldStatus)) {
                throw new RuntimeException("Order must be in OUT_FOR_DELIVERY state to be delivered.");
            }
            // Strict payment check on backend
            if (order.getPaymentstatus() == null || !"SUCCESS".equalsIgnoreCase(order.getPaymentstatus())) {
                throw new RuntimeException("Order cannot be delivered unless payment status is SUCCESS.");
            }
        }

        String targetStatus = oldStatus;

        String performedBy = "System";
        String trackingNumber = null;
        String vehicleNumber = null;

        if (payload != null) {
            if (payload.containsKey("performedBy")) {
                performedBy = (String) payload.get("performedBy");
            } else if (payload.containsKey("updatedBy")) {
                performedBy = (String) payload.get("updatedBy");
            }
            trackingNumber = (String) payload.get("trackingNumber");
            vehicleNumber = (String) payload.get("vehicleNumber");
        }

        if (nextStatus.equalsIgnoreCase("PACK")) {
            targetStatus = "READY_FOR_DISPATCH";
        } else if (nextStatus.equalsIgnoreCase("DISPATCH")) {
            // Get dispatches map from payload
            Map<?, ?> dispatchesMap = null;
            if (payload != null && payload.containsKey("dispatches")) {
                Object d = payload.get("dispatches");
                if (d instanceof Map) {
                    dispatchesMap = (Map<?, ?>) d;
                }
            }

            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            for (OrderItem item : items) {
                int remainingQty = item.getQuantity() - item.getDispatchedQuantity();
                if (remainingQty <= 0) {
                    continue;
                }

                int dispatchQty = remainingQty; // Default to all remaining
                if (dispatchesMap != null) {
                    String prodIdStr = item.getProduct().getId().toString();
                    if (dispatchesMap.containsKey(prodIdStr)) {
                        dispatchQty = ((Number) dispatchesMap.get(prodIdStr)).intValue();
                    } else if (dispatchesMap.containsKey(item.getProduct().getId())) {
                        dispatchQty = ((Number) dispatchesMap.get(item.getProduct().getId())).intValue();
                    }
                }

                // Bound check dispatchQty
                if (dispatchQty < 0) {
                    dispatchQty = 0;
                }
                if (dispatchQty > remainingQty) {
                    dispatchQty = remainingQty;
                }

                if (dispatchQty > 0) {
                    // Update dispatched quantity
                    item.setDispatchedQuantity(item.getDispatchedQuantity() + dispatchQty);
                    orderItemRepository.save(item);

                    // Deduct from product stock
                    Product product = item.getProduct();
                    int newProductQty = product.getQuantity() - dispatchQty;
                    if (newProductQty < 0) {
                        newProductQty = 0;
                    }
                    product.setQuantity(newProductQty);
                    productRepository.save(product);
                }
            }

            // Determine targetStatus based on completeness of dispatches
            boolean allDispatched = true;
            for (OrderItem item : items) {
                if (item.getDispatchedQuantity() < item.getQuantity()) {
                    allDispatched = false;
                    break;
                }
            }

            targetStatus = allDispatched ? "DISPATCHED" : "PARTIALLY_DISPATCHED";

            DispatchDetails dispatch = dispatchDetailsRepository.findByOrderId(orderId)
                    .orElse(new DispatchDetails());
            dispatch.setOrderId(orderId);
            dispatch.setTrackingNumber(trackingNumber);
            dispatch.setVehicleNumber(vehicleNumber);
            dispatch.setDispatchDate(LocalDateTime.now());
            dispatchDetailsRepository.save(dispatch);
            
        } else if (nextStatus.equalsIgnoreCase("OUT_FOR_DELIVERY")) {
            targetStatus = "OUT_FOR_DELIVERY";
        } else if (nextStatus.equalsIgnoreCase("DELIVER")) {
            targetStatus = "DELIVERED";
            
            DeliveryDetails delivery = deliveryDetailsRepository.findByOrderId(orderId)
                    .orElse(new DeliveryDetails());
            delivery.setOrderId(orderId);
            delivery.setDeliveryStatus("DELIVERED");
            delivery.setDeliveredAt(LocalDateTime.now());
            deliveryDetailsRepository.save(delivery);
        }

        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        try {
            socketIOService.emitOrderStatusUpdate(order.getOrderCode(), order.getStatus());
            socketIOService.emitNotification("Order Status Updated", "Order " + order.getOrderCode() + " is now " + order.getStatus() + ".", "info");
        } catch (Exception e) {
            System.err.println("Failed to emit workflow order status update socket event: " + e.getMessage());
        }

        // Record history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(targetStatus);
        history.setUpdatedBy(performedBy);
        history.setUpdatedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);

        // Send email notification for status change
        try {
            if ("DELIVERED".equalsIgnoreCase(targetStatus)) {
                emailService.sendDeliveryConfirmationEmail(order);
            } else {
                emailService.sendOrderStatusEmail(order, oldStatus, targetStatus);
            }
        } catch (Exception e) {
            System.err.println("Failed to send status email: " + e.getMessage());
        }

        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Order transitioned successfully to: " + targetStatus, order));
    }

    @PostMapping("/orders/{orderId}/request-partial-approval")
    public ResponseEntity<ApiResponse> requestPartialApproval(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Integer> proposedQuantities) {
        
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"CREATED".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Partial approval can only be requested for PENDING or CREATED orders.");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            String prodIdStr = item.getProduct().getId().toString();
            if (proposedQuantities.containsKey(prodIdStr)) {
                int proposed = proposedQuantities.get(prodIdStr);
                if (proposed < 0 || proposed > item.getQuantity()) {
                    throw new RuntimeException("Invalid proposed quantity: " + proposed + " for product " + item.getProductName());
                }
                item.setProposedQuantity(proposed);
                orderItemRepository.save(item);
            } else {
                item.setProposedQuantity(item.getQuantity());
                orderItemRepository.save(item);
            }
        }

        String oldStatus = order.getStatus();
        order.setStatus("AWAITING_PARTIAL_APPROVAL");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Record history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus("AWAITING_PARTIAL_APPROVAL");
        history.setUpdatedBy(username != null ? username : "System");
        history.setUpdatedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);

        try {
            socketIOService.emitOrderStatusUpdate(order.getOrderCode(), order.getStatus());
            socketIOService.emitNotification("Partial Approval Requested", "Order " + order.getOrderCode() + " is awaiting partial fulfillment approval.", "info");
        } catch (Exception e) {
            System.err.println("Failed to emit socket event: " + e.getMessage());
        }

        // Send partial approval email to customer
        try {
            emailService.sendPartialApprovalEmail(order, items);
        } catch (Exception e) {
            System.err.println("Failed to send partial approval email: " + e.getMessage());
        }

        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Partial approval requested successfully", order));
    }

    @PostMapping("/orders/{orderId}/approve-partial")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse> approvePartial(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!"AWAITING_PARTIAL_APPROVAL".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Order is not awaiting partial approval.");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        
        List<OrderItem> backorderItems = new ArrayList<>();
        boolean needsSplit = false;

        double originalNewTotal = 0.0;
        double originalNewBaseTotal = 0.0;
        double originalNewTax = 0.0;
        double originalNewDiscount = 0.0;

        double backorderTotal = 0.0;
        double backorderBaseTotal = 0.0;
        double backorderTax = 0.0;
        double backorderDiscount = 0.0;

        for (OrderItem item : items) {
            int proposed = item.getProposedQuantity() != null ? item.getProposedQuantity() : item.getQuantity();
            int remaining = item.getQuantity() - proposed;

            double pricePerUnit = item.getPrice();
            double itemDiscountRate = item.getDiscount();
            double gstRate = item.getGstpercentage();

            double origBaseVal = pricePerUnit * proposed;
            double origDiscVal = origBaseVal * (itemDiscountRate / 100.0);
            double origTaxableVal = origBaseVal - origDiscVal;
            double origTaxVal = origTaxableVal * (gstRate / 100.0);
            double origTotalVal = origTaxableVal + origTaxVal;

            item.setQuantity(proposed);
            item.setTotalPrice(origTotalVal);
            item.setTaxamount(origTaxVal);
            item.setProposedQuantity(null); 
            orderItemRepository.save(item);

            originalNewBaseTotal += origBaseVal;
            originalNewDiscount += origDiscVal;
            originalNewTax += origTaxVal;
            originalNewTotal += origTotalVal;

            if (remaining > 0) {
                needsSplit = true;

                OrderItem boItem = new OrderItem();
                boItem.setProduct(item.getProduct());
                boItem.setProductName(item.getProductName());
                boItem.setProductCode(item.getProductCode());
                boItem.setDivisionName(item.getDivisionName());
                boItem.setQuantity(remaining);
                boItem.setPrice(pricePerUnit);
                boItem.setDiscount(itemDiscountRate);
                boItem.setGstpercentage(gstRate);
                boItem.setDispatchedQuantity(0);
                boItem.setStatus("PENDING");

                double boBaseVal = pricePerUnit * remaining;
                double boDiscVal = boBaseVal * (itemDiscountRate / 100.0);
                double boTaxableVal = boBaseVal - boDiscVal;
                double boTaxVal = boTaxableVal * (gstRate / 100.0);
                double boTotalVal = boTaxableVal + boTaxVal;

                boItem.setTaxamount(boTaxVal);
                boItem.setTotalPrice(boTotalVal);
                
                backorderItems.add(boItem);

                backorderBaseTotal += boBaseVal;
                backorderDiscount += boDiscVal;
                backorderTax += boTaxVal;
                backorderTotal += boTotalVal;
            }
        }

        order.setBaseTotal(originalNewBaseTotal);
        order.setTotalDiscount(originalNewDiscount);
        order.setTotalTax(originalNewTax);
        order.setTotalPrice(originalNewTotal);
        String oldStatus = order.getStatus();
        order.setStatus("READY_FOR_DISPATCH"); 
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus("READY_FOR_DISPATCH");
        history.setUpdatedBy(username != null ? username : "System");
        history.setUpdatedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);

        Order backorder = null;
        if (needsSplit) {
            backorder = new Order();
            String boCode = order.getOrderCode() + "-BO";
            if (orderRepository.findByOrderCode(boCode) != null) {
                boCode = order.getOrderCode() + "-BO-" + (System.currentTimeMillis() % 1000);
            }
            backorder.setOrderCode(boCode);
            backorder.setCustomer(order.getCustomer());
            backorder.setCustomerName(order.getCustomerName());
            backorder.setCustomerEmail(order.getCustomerEmail());
            backorder.setShippingAddress(order.getShippingAddress());
            backorder.setStatus("PENDING");
            backorder.setPaymentstatus("PENDING");
            backorder.setBaseTotal(backorderBaseTotal);
            backorder.setTotalDiscount(backorderDiscount);
            backorder.setTotalTax(backorderTax);
            backorder.setTotalPrice(backorderTotal);
            backorder.setOrderDate(LocalDateTime.now());
            backorder.setUpdatedAt(LocalDateTime.now());
            
            Order savedBackorder = orderRepository.save(backorder);

            for (OrderItem boItem : backorderItems) {
                boItem.setOrder(savedBackorder);
                orderItemRepository.save(boItem);
            }

            OrderStatusHistory boHistory = new OrderStatusHistory();
            boHistory.setOrderId(savedBackorder.getId());
            boHistory.setOldStatus("CREATED");
            boHistory.setNewStatus("PENDING");
            boHistory.setUpdatedBy("System");
            boHistory.setUpdatedAt(LocalDateTime.now());
            orderStatusHistoryRepository.save(boHistory);
        }

        try {
            socketIOService.emitOrderStatusUpdate(order.getOrderCode(), "READY_FOR_DISPATCH");
            socketIOService.emitNotification("Partial Order Approved", "Order " + order.getOrderCode() + " was approved for partial shipment.", "success");
            if (backorder != null) {
                socketIOService.emitNotification("Backorder Created", "Backorder " + backorder.getOrderCode() + " has been created for remaining items.", "info");
            }
        } catch (Exception e) {
            System.err.println("Failed to emit socket: " + e.getMessage());
        }

        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Partial approval approved. Original order is now READY_FOR_DISPATCH. Backorder created.", order));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<ApiResponse> getWarehouses() {
        List<Warehouse> list = warehouseRepository.findAll();
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", list));
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse> getWarehouseStocks() {
        List<WarehouseStock> list = warehouseStockRepository.findAll();
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", list));
    }

    @PostMapping("/stock/add")
    public ResponseEntity<ApiResponse> addWarehouseStock(
            @RequestParam Long warehouseId,
            @RequestParam Long productId,
            @RequestParam int quantity) {
        
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + warehouseId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        WarehouseStock ws = warehouseStockRepository.findByWarehouse_IdAndProduct_Id(warehouseId, productId)
                .orElse(new WarehouseStock());
        
        if (ws.getId() == null) {
            ws.setWarehouse(warehouse);
            ws.setProduct(product);
            ws.setAvailableQty(quantity);
            ws.setReservedQty(0);
        } else {
            ws.setAvailableQty(ws.getAvailableQty() + quantity);
        }

        warehouseStockRepository.save(ws);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Stock added successfully", ws));
    }

    @PostMapping("/stock/transfer")
    public ResponseEntity<ApiResponse> transferWarehouseStock(
            @RequestParam Long fromWarehouseId,
            @RequestParam Long toWarehouseId,
            @RequestParam Long productId,
            @RequestParam int quantity) {

        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be a positive number");
        }
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new RuntimeException("Source and destination warehouses must be different");
        }

        WarehouseStock sourceStock = warehouseStockRepository.findByWarehouse_IdAndProduct_Id(fromWarehouseId, productId)
                .orElseThrow(() -> new RuntimeException("Source stock not found"));

        if (sourceStock.getAvailableQty() < quantity) {
            throw new RuntimeException("Insufficient stock in source warehouse");
        }

        Warehouse toWarehouse = warehouseRepository.findById(toWarehouseId)
                .orElseThrow(() -> new RuntimeException("Destination warehouse not found: " + toWarehouseId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        WarehouseStock destStock = warehouseStockRepository.findByWarehouse_IdAndProduct_Id(toWarehouseId, productId)
                .orElse(new WarehouseStock());

        // Deduct from source
        sourceStock.setAvailableQty(sourceStock.getAvailableQty() - quantity);
        warehouseStockRepository.save(sourceStock);

        // Add to destination
        if (destStock.getId() == null) {
            destStock.setWarehouse(toWarehouse);
            destStock.setProduct(product);
            destStock.setAvailableQty(quantity);
            destStock.setReservedQty(0);
        } else {
            destStock.setAvailableQty(destStock.getAvailableQty() + quantity);
        }
        warehouseStockRepository.save(destStock);

        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Stock transferred successfully", destStock));
    }

    @PostMapping("/warehouses/add")
    public ResponseEntity<ApiResponse> addWarehouse(
            @RequestParam String name,
            @RequestParam String location) {
        
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setLocation(location);
        
        warehouseRepository.save(warehouse);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Warehouse registered successfully", warehouse));
    }
}
