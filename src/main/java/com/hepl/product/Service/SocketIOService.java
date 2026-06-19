package com.hepl.product.Service;

import com.corundumstudio.socketio.SocketIOServer;
import com.hepl.product.Payload.Dto.OrderDto.OrderResponseDto;
import com.hepl.product.Payload.Dto.StockDto.StockRespnseDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SocketIOService {

    private final SocketIOServer server;

    @Autowired
    public SocketIOService(SocketIOServer server) {
        this.server = server;
    }

    @PostConstruct
    public void startServer() {
        // Logging/Debugging connect events
        server.addConnectListener(client -> {
            log.info("Socket.IO client connected: session ID = {}, IP = {}", 
                client.getSessionId(), client.getRemoteAddress());
        });

        // Logging/Debugging disconnect events
        server.addDisconnectListener(client -> {
            log.info("Socket.IO client disconnected: session ID = {}", client.getSessionId());
        });

        server.start();
        log.info("Socket.IO Server started successfully on port: {}", server.getConfiguration().getPort());
    }

    private static final ThreadLocal<Boolean> suppressNotifications = ThreadLocal.withInitial(() -> false);

    public static void setNotificationSuppression(boolean suppress) {
        suppressNotifications.set(suppress);
    }

    public static void clearNotificationSuppression() {
        suppressNotifications.remove();
    }

    public static boolean isNotificationSuppressed() {
        return suppressNotifications.get();
    }

    @PreDestroy
    public void stopServer() {
        log.info("Stopping Socket.IO Server...");
        server.stop();
    }

    public void emitOrderStatusUpdate(String orderCode, String newStatus) {
        if (isNotificationSuppressed()) return;
        OrderStatusEvent event = new OrderStatusEvent(orderCode, newStatus);
        log.info("Emitting orderStatusUpdated event for orderCode = {} to status = {}", orderCode, newStatus);
        server.getBroadcastOperations().sendEvent("orderStatusUpdated", event);
    }

    public void emitOrderPaymentUpdate(String orderCode, String paymentStatus) {
        if (isNotificationSuppressed()) return;
        OrderPaymentEvent event = new OrderPaymentEvent(orderCode, paymentStatus);
        log.info("Emitting orderPaymentUpdated event for orderCode = {} to paymentStatus = {}", orderCode, paymentStatus);
        server.getBroadcastOperations().sendEvent("orderPaymentUpdated", event);
    }

    public void emitOrderCreated(OrderResponseDto order) {
        if (isNotificationSuppressed()) return;
        log.info("Emitting orderCreated event for orderCode = {}", order.getOrderCode());
        server.getBroadcastOperations().sendEvent("orderCreated", order);
    }

    public void emitStockUpdated(StockRespnseDto stock) {
        if (isNotificationSuppressed()) return;
        log.info("Emitting stockUpdated event for product = {}", stock.getProductName());
        server.getBroadcastOperations().sendEvent("stockUpdated", stock);
    }

    public void emitNotification(String title, String message, String type) {
        if (isNotificationSuppressed()) return;
        NotificationEvent event = new NotificationEvent(title, message, type);
        log.info("Emitting newNotification event: title = '{}', message = '{}', type = '{}'", title, message, type);
        server.getBroadcastOperations().sendEvent("newNotification", event);
    }

    // Inner class for the order status event payload
    public static class OrderStatusEvent {
        private String orderCode;
        private String status;

        public OrderStatusEvent(String orderCode, String status) {
            this.orderCode = orderCode;
            this.status = status;
        }

        public String getOrderCode() {
            return orderCode;
        }

        public void setOrderCode(String orderCode) {
            this.orderCode = orderCode;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // Inner class for the order payment event payload
    public static class OrderPaymentEvent {
        private String orderCode;
        private String paymentStatus;

        public OrderPaymentEvent(String orderCode, String paymentStatus) {
            this.orderCode = orderCode;
            this.paymentStatus = paymentStatus;
        }

        public String getOrderCode() {
            return orderCode;
        }

        public void setOrderCode(String orderCode) {
            this.orderCode = orderCode;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public void setPaymentStatus(String paymentStatus) {
            this.paymentStatus = paymentStatus;
        }
    }

    // Inner class for notification events
    public static class NotificationEvent {
        private String title;
        private String message;
        private String type;
        private long timestamp;

        public NotificationEvent(String title, String message, String type) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
