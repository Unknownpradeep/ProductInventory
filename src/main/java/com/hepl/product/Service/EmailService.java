package com.hepl.product.Service;

import com.hepl.product.model.Order;
import com.hepl.product.model.OrderItem;

import java.util.List;

public interface EmailService {

    /**
     * Send a generic HTML email
     */
    void sendEmail(String to, String subject, String htmlBody);

    /**
     * Send a generic HTML email with an attachment
     */
    void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] attachmentBytes, String attachmentName);

    /**
     * Send order confirmation email after a new B2B order is created
     */
    void sendOrderCreatedEmail(Order order);

    /**
     * Send notification when order transitions to a new status
     */
    void sendOrderStatusEmail(Order order, String oldStatus, String newStatus);

    /**
     * Send partial approval notification email
     */
    void sendPartialApprovalEmail(Order order, List<OrderItem> items);

    /**
     * Send delivery confirmation email
     */
    void sendDeliveryConfirmationEmail(Order order);
}
