package com.hepl.product.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Repository.OrderItemRepository;
import com.hepl.product.Service.EmailService;
import com.hepl.product.model.Order;
import com.hepl.product.model.OrderItem;

@RestController
@RequestMapping("/api/v1/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Send a custom test email
     * POST /api/v1/email/send
     * Body: { "to": "test@example.com", "subject": "Test", "body": "<h1>Hello</h1>" }
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse> sendTestEmail(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        if (to == null || subject == null || body == null) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Missing required fields: to, subject, body", null));
        }

        emailService.sendEmail(to, subject, body);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Email sent successfully to " + to, null));
    }

    /**
     * Resend order confirmation email
     * POST /api/v1/email/order-confirmation/{orderId}
     */
    @PostMapping("/order-confirmation/{orderId}")
    public ResponseEntity<ApiResponse> resendOrderConfirmation(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        emailService.sendOrderCreatedEmail(order);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(),
            "Order confirmation email sent for " + order.getOrderCode(), null));
    }

    /**
     * Send order status update email
     * POST /api/v1/email/status-update/{orderId}
     * Body: { "oldStatus": "PENDING", "newStatus": "READY_FOR_DISPATCH" }
     */
    @PostMapping("/status-update/{orderId}")
    public ResponseEntity<ApiResponse> sendStatusUpdateEmail(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> payload) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String oldStatus = payload.getOrDefault("oldStatus", "PENDING");
        String newStatus = payload.getOrDefault("newStatus", order.getStatus());

        emailService.sendOrderStatusEmail(order, oldStatus, newStatus);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(),
            "Status update email sent for " + order.getOrderCode(), null));
    }

    /**
     * Send partial approval email
     * POST /api/v1/email/partial-approval/{orderId}
     */
    @PostMapping("/partial-approval/{orderId}")
    public ResponseEntity<ApiResponse> sendPartialApprovalEmail(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        emailService.sendPartialApprovalEmail(order, items);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(),
            "Partial approval email sent for " + order.getOrderCode(), null));
    }
    /**
     * Send delivery confirmation email
     * POST /api/v1/email/delivery-confirmation/{orderId}
     */
    @PostMapping("/delivery-confirmation/{orderId}")
    public ResponseEntity<ApiResponse> sendDeliveryConfirmationEmail(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        emailService.sendDeliveryConfirmationEmail(order);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(),
            "Delivery confirmation email sent for " + order.getOrderCode(), null));
    }

    @Autowired
    private com.hepl.product.Scheduler.SalesSummaryScheduler salesSummaryScheduler;

    @PostMapping("/trigger-sales-report")
    public ResponseEntity<ApiResponse> triggerSalesReport() {
        salesSummaryScheduler.sendDailySalesReport();
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(),
            "Daily sales & inventory report triggered successfully", null));
    }

    /**
     * Download the daily enterprise sales report as a PDF without sending email.
     * GET /api/v1/email/download-sales-report
     */
    @GetMapping("/download-sales-report")
    public ResponseEntity<byte[]> downloadSalesReport() {
        try {
            byte[] pdfBytes = salesSummaryScheduler.generatePdfForDownload();
            String filename = "Daily_Enterprise_Sales_Report_" + LocalDate.now() + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
            );
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
