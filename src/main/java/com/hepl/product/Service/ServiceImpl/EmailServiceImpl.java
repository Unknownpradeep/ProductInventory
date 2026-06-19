package com.hepl.product.Service.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hepl.product.Service.EmailService;
import com.hepl.product.model.Order;
import com.hepl.product.model.OrderItem;

import jakarta.mail.internet.MimeMessage;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    static {
        System.setProperty("mail.mime.doublequotes", "true");
        System.setProperty("mail.mime.parameters.strict", "false");
        System.setProperty("mail.mime.decodeparameters", "true");
        System.setProperty("mail.mime.encodeparameters", "true");
    }

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private com.hepl.product.Repository.DispatchDetailsRepository dispatchDetailsRepository;

    @Autowired
    private com.hepl.product.Repository.InvoiceRepository invoiceRepository;

    @Autowired
    private com.hepl.product.Repository.OrderRepository orderRepository;

    @Autowired
    private com.hepl.product.Service.InvoiceService invoiceService;

    @Value("${app.mail.from:inventory@hepl-product.com}")
    private String fromAddress;

    @Value("${app.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Override
    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("[EmailService] ✔ Email sent to: " + to + " | Subject: " + subject);
        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                java.nio.file.Files.writeString(java.nio.file.Paths.get("C:\\Users\\acer\\Desktop\\PRODUCT\\email_error.txt"), sw.toString());
            } catch (Exception fe) {}
        }
    }

    @Override
    @Async
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] attachmentBytes, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (attachmentBytes != null && attachmentBytes.length > 0) {
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(attachmentBytes);
                helper.addAttachment(attachmentName, resource, "application/pdf");
            }
            mailSender.send(message);
            System.out.println("[EmailService] ✔ Email with attachment sent to: " + to + " | Subject: " + subject);
        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Failed to send email with attachment to " + to + ": " + e.getMessage());
            e.printStackTrace();
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                java.nio.file.Files.writeString(java.nio.file.Paths.get("C:\\Users\\acer\\Desktop\\PRODUCT\\email_error.txt"), sw.toString());
            } catch (Exception fe) {}
        }
    }

    @Override
    @Async
    @Transactional
    public void sendOrderCreatedEmail(Order order) {
        String to = order.getCustomerEmail();
        if (to == null || to.isBlank()) return;

        byte[] pdfBytes = null;
        String attachmentName = "Invoice_" + order.getOrderCode() + ".pdf";
        com.hepl.product.model.Invoice invoice = null;
        try {
            order = orderRepository.findById(order.getId()).orElse(order);
            if (order.getOrderItems() != null) {
                order.getOrderItems().size();
            }
            invoice = invoiceRepository.findByOrderCode(order.getOrderCode())
                .orElse(null);
            if (invoice == null) {
                com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto dto = invoiceService.generateInvoice(order.getOrderCode());
                invoice = invoiceRepository.findById(dto.getId()).orElse(null);
            }
            if (invoice != null) {
                if (invoice.getOrder() != null && invoice.getOrder().getOrderItems() != null) {
                    invoice.getOrder().getOrderItems().size();
                }
                pdfBytes = invoiceService.exportToPdf(java.util.List.of(invoice), "CLASSIC");
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to generate/fetch invoice PDF for email: " + e.getMessage());
            e.printStackTrace();
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                java.nio.file.Files.writeString(java.nio.file.Paths.get("C:\\Users\\acer\\Desktop\\PRODUCT\\email_error.txt"), sw.toString());
            } catch (Exception fe) {}
        }

        String subject = "📦 Order Confirmed - " + order.getOrderCode();
        String html = buildOrderCreatedHtml(order, invoice);

        if (pdfBytes != null) {
            sendEmailWithAttachment(to, subject, html, pdfBytes, attachmentName);
        } else {
            sendEmail(to, subject, html);
        }
    }

    @Override
    @Async
    @Transactional
    public void sendOrderStatusEmail(Order order, String oldStatus, String newStatus) {
        String to = order.getCustomerEmail();
        if (to == null || to.isBlank()) return;

        byte[] pdfBytes = null;
        String attachmentName = "Invoice_" + order.getOrderCode() + ".pdf";
        com.hepl.product.model.Invoice invoice = null;
        try {
            order = orderRepository.findById(order.getId()).orElse(order);
            if (order.getOrderItems() != null) {
                order.getOrderItems().size();
            }
            invoice = invoiceRepository.findByOrderCode(order.getOrderCode())
                .orElse(null);
            if (invoice == null) {
                com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto dto = invoiceService.generateInvoice(order.getOrderCode());
                invoice = invoiceRepository.findById(dto.getId()).orElse(null);
            }
            if (invoice != null) {
                if (invoice.getOrder() != null && invoice.getOrder().getOrderItems() != null) {
                    invoice.getOrder().getOrderItems().size();
                }
                pdfBytes = invoiceService.exportToPdf(java.util.List.of(invoice), "CLASSIC");
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to generate/fetch invoice PDF for email: " + e.getMessage());
            e.printStackTrace();
        }

        String subject = "📋 Order Update - " + order.getOrderCode() + " → " + newStatus;
        String html = buildStatusUpdateHtml(order, oldStatus, newStatus);

        if (pdfBytes != null) {
            sendEmailWithAttachment(to, subject, html, pdfBytes, attachmentName);
        } else {
            sendEmail(to, subject, html);
        }
    }

    @Override
    @Async
    @Transactional
    public void sendPartialApprovalEmail(Order order, List<OrderItem> items) {
        String to = order.getCustomerEmail();
        if (to == null || to.isBlank()) return;

        byte[] pdfBytes = null;
        String attachmentName = "Invoice_" + order.getOrderCode() + ".pdf";
        com.hepl.product.model.Invoice invoice = null;
        try {
            order = orderRepository.findById(order.getId()).orElse(order);
            if (order.getOrderItems() != null) {
                order.getOrderItems().size();
            }
            invoice = invoiceRepository.findByOrderCode(order.getOrderCode())
                .orElse(null);
            if (invoice == null) {
                com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto dto = invoiceService.generateInvoice(order.getOrderCode());
                invoice = invoiceRepository.findById(dto.getId()).orElse(null);
            }
            if (invoice != null) {
                if (invoice.getOrder() != null && invoice.getOrder().getOrderItems() != null) {
                    invoice.getOrder().getOrderItems().size();
                }
                pdfBytes = invoiceService.exportToPdf(java.util.List.of(invoice), "CLASSIC");
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to generate/fetch invoice PDF for email: " + e.getMessage());
            e.printStackTrace();
        }

        String subject = "⚠️ Partial Shipment Approval Required - " + order.getOrderCode();
        String html = buildPartialApprovalHtml(order, items);

        if (pdfBytes != null) {
            sendEmailWithAttachment(to, subject, html, pdfBytes, attachmentName);
        } else {
            sendEmail(to, subject, html);
        }
    }

    @Override
    @Async
    @Transactional
    public void sendDeliveryConfirmationEmail(Order order) {
        String to = order.getCustomerEmail();
        if (to == null || to.isBlank()) return;

        byte[] pdfBytes = null;
        String attachmentName = "Invoice_" + order.getOrderCode() + ".pdf";
        com.hepl.product.model.Invoice invoice = null;
        try {
            order = orderRepository.findById(order.getId()).orElse(order);
            if (order.getOrderItems() != null) {
                order.getOrderItems().size();
            }
            invoice = invoiceRepository.findByOrderCode(order.getOrderCode())
                .orElse(null);
            if (invoice == null) {
                com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto dto = invoiceService.generateInvoice(order.getOrderCode());
                invoice = invoiceRepository.findById(dto.getId()).orElse(null);
            }
            if (invoice != null) {
                if (invoice.getOrder() != null && invoice.getOrder().getOrderItems() != null) {
                    invoice.getOrder().getOrderItems().size();
                }
                pdfBytes = invoiceService.exportToPdf(java.util.List.of(invoice), "CLASSIC");
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to generate/fetch invoice PDF for email: " + e.getMessage());
            e.printStackTrace();
        }

        String subject = "✅ Order Delivered - " + order.getOrderCode();
        String html = buildDeliveryConfirmationHtml(order, invoice);

        if (pdfBytes != null) {
            sendEmailWithAttachment(to, subject, html, pdfBytes, attachmentName);
        } else {
            sendEmail(to, subject, html);
        }
    }

    // ────────────────── HTML Template Builders ──────────────────

    private String baseWrapper(String title, String bodyContent) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#ffffff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#ffffff;padding:20px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border:1px solid #e2e8f0;border-radius:6px;overflow:hidden;">
                                <tr>
                                    <td style="padding:24px 30px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="font-size:20px;font-weight:700;color:#1e3a8a;padding-bottom:12px;border-bottom:1px solid #e2e8f0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding-top:15px;font-size:14px;color:#334155;line-height:1.5;">
                                                    %s
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background:#f8fafc;padding:16px 30px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
                                        This is an automated notification from the HEPL Inventory Management System. Please do not reply directly to this email.
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(title, bodyContent);
    }

    private String buildOrderCreatedHtml(Order order, com.hepl.product.model.Invoice invoice) {
        StringBuilder items = new StringBuilder();
        if (order.getOrderItems() != null) {
            items.append("<table width='100%%' cellpadding='0' cellspacing='0' style='margin-top:20px;border:1px solid #e2e8f0;border-radius:4px;'>");
            items.append("<tr style='background:#f8fafc;'><th align='left' style='padding:10px 12px;font-size:12px;color:#334155;font-weight:700;border-bottom:1px solid #e2e8f0;'>Product</th><th align='center' style='padding:10px 12px;font-size:12px;color:#334155;font-weight:700;border-bottom:1px solid #e2e8f0;width:60px;'>Qty</th><th align='right' style='padding:10px 12px;font-size:12px;color:#334155;font-weight:700;border-bottom:1px solid #e2e8f0;width:100px;'>Amount</th></tr>");
            for (OrderItem item : order.getOrderItems()) {
                items.append(String.format(
                    "<tr><td style='padding:10px 12px;font-size:13px;color:#334155;border-bottom:1px solid #f1f5f9;'>%s</td><td align='center' style='padding:10px 12px;font-size:13px;color:#334155;border-bottom:1px solid #f1f5f9;'>%d</td><td align='right' style='padding:10px 12px;font-size:13px;color:#0f172a;font-weight:600;border-bottom:1px solid #f1f5f9;'>₹%.2f</td></tr>",
                    item.getProductName(), item.getQuantity(), item.getTotalPrice()
                ));
            }
            items.append("</table>");
        }

        String invoiceCode = invoice != null ? invoice.getInvoiceCode() : "N/A";
        String invoiceStatus = invoice != null ? invoice.getStatus() : "PENDING";
        String invoiceDate = (invoice != null && invoice.getInvoiceDate() != null) ? invoice.getInvoiceDate().toLocalDate().toString() : "N/A";

        String body = String.format("""
            <p style="margin:0 0 16px;">Your purchase order has been successfully created and is now being processed. A copy of your invoice is attached to this email as a PDF.</p>
            
            <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e2e8f0;border-radius:4px;margin-bottom:16px;">
                <tr style="background:#f8fafc;"><td colspan="2" style="padding:10px 12px;font-weight:700;color:#1e3a8a;font-size:12px;text-transform:uppercase;border-bottom:1px solid #e2e8f0;">Order Details</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Order Code:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Customer:</td><td align="right" style="padding:10px 12px;font-size:13px;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Order Status:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#1e3a8a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;">Total Amount:</td><td align="right" style="padding:10px 12px;font-size:14px;font-weight:700;color:#0f172a;">₹%.2f</td></tr>
            </table>

            <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e2e8f0;border-radius:4px;margin-bottom:16px;">
                <tr style="background:#f8fafc;"><td colspan="2" style="padding:10px 12px;font-weight:700;color:#1e3a8a;font-size:12px;text-transform:uppercase;border-bottom:1px solid #e2e8f0;">Invoice Summary</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Invoice Code:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Invoice Date:</td><td align="right" style="padding:10px 12px;font-size:13px;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;">Invoice Status:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#1e3a8a;">%s</td></tr>
            </table>

            <div style="margin:20px 0;text-align:center;">
                <a href="%s/api/v1/invoices/download/order/%s" style="display:inline-block;padding:10px 20px;background:#1e3a8a;color:#ffffff;text-decoration:none;font-weight:700;border-radius:4px;font-size:13px;text-align:center;">Download Invoice PDF</a>
            </div>
            %s
            """, 
            order.getOrderCode(), 
            order.getCustomerName(), 
            order.getStatus(), 
            order.getTotalPrice(), 
            invoiceCode, 
            invoiceDate, 
            invoiceStatus, 
            this.apiBaseUrl,
            order.getOrderCode(),
            items.toString());

        return baseWrapper("Order Confirmed", body);
    }

    private String buildStatusUpdateHtml(Order order, String oldStatus, String newStatus) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(String.format("""
            <p style="margin:0 0 16px;">Your order status has been updated.</p>
            
            <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e2e8f0;border-radius:4px;margin-bottom:16px;">
                <tr style="background:#f8fafc;"><td colspan="2" style="padding:10px 12px;font-weight:700;color:#1e3a8a;font-size:12px;text-transform:uppercase;border-bottom:1px solid #e2e8f0;">Status Update</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Order Reference:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Previous Status:</td><td align="right" style="padding:10px 12px;font-size:13px;color:#4b5563;font-weight:600;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;">Current Status:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#1e3a8a;">%s</td></tr>
            </table>
            """, order.getOrderCode(), oldStatus, newStatus));

        bodyBuilder.append(String.format("""
            <div style="margin:20px 0;text-align:center;">
                <a href="%s/api/v1/invoices/download/order/%s" style="display:inline-block;padding:10px 20px;background:#1e3a8a;color:#ffffff;text-decoration:none;font-weight:700;border-radius:4px;font-size:13px;text-align:center;">Download Invoice PDF</a>
            </div>
            """, this.apiBaseUrl, order.getOrderCode()));

        if ("DISPATCHED".equalsIgnoreCase(newStatus) || "PARTIALLY_DISPATCHED".equalsIgnoreCase(newStatus) || "SHIPPED".equalsIgnoreCase(newStatus)) {
            try {
                java.util.Optional<com.hepl.product.model.DispatchDetails> dispatchOpt = dispatchDetailsRepository.findByOrderId(order.getId());
                if (dispatchOpt.isPresent()) {
                    com.hepl.product.model.DispatchDetails details = dispatchOpt.get();
                    String tracking = details.getTrackingNumber() != null ? details.getTrackingNumber() : "N/A";
                    String vehicle = details.getVehicleNumber() != null ? details.getVehicleNumber() : "N/A";
                    bodyBuilder.append(String.format("""
                        <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e2e8f0;border-radius:4px;margin-bottom:16px;">
                            <tr style="background:#f8fafc;"><td colspan="2" style="padding:10px 12px;font-weight:700;color:#1e3a8a;font-size:12px;text-transform:uppercase;border-bottom:1px solid #e2e8f0;">🚚 Shipment Details</td></tr>
                            <tr><td style="padding:8px 12px;font-size:13px;color:#6b7280;border-bottom:1px solid #f1f5f9;">Tracking ID:</td><td align="right" style="padding:8px 12px;font-size:13px;font-weight:700;color:#111827;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                            <tr><td style="padding:8px 12px;font-size:13px;color:#6b7280;">Vehicle Registration Number:</td><td align="right" style="padding:8px 12px;font-size:13px;font-weight:700;color:#111827;">%s</td></tr>
                        </table>
                        """, tracking, vehicle));
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch dispatch details for email: " + e.getMessage());
            }
        }

        return baseWrapper("Order Status Updated", bodyBuilder.toString());
    }

    private String buildPartialApprovalHtml(Order order, List<OrderItem> items) {
        StringBuilder table = new StringBuilder();
        table.append("<table width='100%%' cellpadding='0' cellspacing='0' style='margin-top:20px;border:1px solid #e2e8f0;border-radius:4px;'>");
        table.append("<tr style='background:#f8fafc;'><th align='left' style='padding:10px 12px;font-size:12px;color:#334155;font-weight:700;border-bottom:1px solid #e2e8f0;'>Product</th><th align='center' style='padding:10px 12px;font-size:12px;color:#334155;font-weight:700;border-bottom:1px solid #e2e8f0;width:80px;'>Ordered</th><th align='center' style='padding:10px 12px;font-size:12px;color:#334155;font-weight:700;border-bottom:1px solid #e2e8f0;width:80px;'>Available</th></tr>");
        for (OrderItem item : items) {
            int proposed = item.getProposedQuantity() != null ? item.getProposedQuantity() : item.getQuantity();
            table.append(String.format(
                "<tr><td style='padding:10px 12px;font-size:13px;color:#334155;border-bottom:1px solid #f1f5f9;'>%s</td><td align='center' style='padding:10px 12px;font-size:13px;color:#334155;border-bottom:1px solid #f1f5f9;'>%d</td><td align='center' style='padding:10px 12px;font-size:13px;font-weight:700;color:#d97706;border-bottom:1px solid #f1f5f9;'>%d</td></tr>",
                item.getProductName(), item.getQuantity(), proposed
            ));
        }
        table.append("</table>");

        String body = String.format("""
            <p style="margin:0 0 16px;">A stock shortage has been detected for your order <strong style="color:#1e3a8a;">%s</strong>. The available quantities are listed below.</p>
            
            <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e2e8f0;border-radius:4px;margin-bottom:16px;">
                <tr>
                    <td style="padding:12px;color:#b45309;font-size:13px;font-weight:700;line-height:1.4;">
                        ⚠️ Action Required: Please review and approve the partial shipment from your dashboard.
                    </td>
                </tr>
            </table>
            
            %s
            
            <p style="margin:16px 0 0;color:#64748b;font-size:12px;line-height:1.5;">The remaining quantities will be auto-created as a separate backorder once approved.</p>
            
            <div style="margin:20px 0;text-align:center;">
                <a href="%s/api/v1/invoices/download/order/%s" style="display:inline-block;padding:10px 20px;background:#1e3a8a;color:#ffffff;text-decoration:none;font-weight:700;border-radius:4px;font-size:13px;text-align:center;">Download Invoice PDF</a>
            </div>
            """, order.getOrderCode(), table.toString(), this.apiBaseUrl, order.getOrderCode());

        return baseWrapper("Partial Shipment Approval Required", body);
    }

    private String buildDeliveryConfirmationHtml(Order order, com.hepl.product.model.Invoice invoice) {
        String invoiceCode = invoice != null ? invoice.getInvoiceCode() : "N/A";
        String invoiceDate = (invoice != null && invoice.getInvoiceDate() != null) ? invoice.getInvoiceDate().toLocalDate().toString() : "N/A";

        String body = String.format("""
            <p style="margin:0 0 16px;">Your order has been successfully delivered! A copy of your invoice is attached to this email as a PDF.</p>
            
            <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e2e8f0;border-radius:4px;margin-bottom:16px;">
                <tr style="background:#f8fafc;"><td colspan="2" style="padding:10px 12px;font-weight:700;color:#1e3a8a;font-size:12px;text-transform:uppercase;border-bottom:1px solid #e2e8f0;">Delivery Details</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Order Code:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Invoice Code:</td><td align="right" style="padding:10px 12px;font-size:13px;font-weight:700;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;">Delivery Date:</td><td align="right" style="padding:10px 12px;font-size:13px;color:#0f172a;border-bottom:1px solid #f1f5f9;">%s</td></tr>
                <tr><td style="padding:10px 12px;font-size:13px;color:#64748b;">Total Paid:</td><td align="right" style="padding:10px 12px;font-size:14px;font-weight:700;color:#059669;">₹%.2f</td></tr>
            </table>

            <div style="margin:20px 0;text-align:center;">
                <a href="%s/api/v1/invoices/download/order/%s" style="display:inline-block;padding:10px 20px;background:#1e3a8a;color:#ffffff;text-decoration:none;font-weight:700;border-radius:4px;font-size:13px;text-align:center;">Download Invoice PDF</a>
            </div>
            
            <p style="margin:16px 0 0;color:#64748b;font-size:12px;line-height:1.5;">Thank you for your business. If you have any questions, please contact our support team.</p>
            """, 
            order.getOrderCode(), 
            invoiceCode, 
            invoiceDate, 
            order.getTotalPrice(), 
            this.apiBaseUrl,
            order.getOrderCode());

        return baseWrapper("Order Delivered", body);
    }
}
