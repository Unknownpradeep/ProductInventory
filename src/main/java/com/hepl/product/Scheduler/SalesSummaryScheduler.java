package com.hepl.product.Scheduler;

import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Service.EmailService;
import com.hepl.product.model.Order;
import com.hepl.product.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;


@Component
public class SalesSummaryScheduler {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.scheduler.sales-report.to:admin@hepl-product.com}")
    private String reportToAddress;

    @Value("${app.scheduler.sales-report.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Value("${app.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Scheduled(cron = "${app.scheduler.sales-report.cron:0 59 23 * * *}")
    public void sendDailySalesReport() {
        try {
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

            // Fetch today's orders
            List<Order> todayOrders = orderRepository.findByOrderDateBetweenAndDeletedFalse(startOfToday, endOfToday);

            // Fetch overall active orders
            List<Order> allOrders = orderRepository.findByDeletedFalse();

            // Fetch low stock items
            List<Product> lowStockProducts = productRepository.findByDeletedFalseAndQuantityLessThan(lowStockThreshold);

            // Calculate Metrics
            int totalOrdersToday = todayOrders.size();
            double totalRevenueToday = todayOrders.stream()
                    .filter(o -> o.getStatus() != null && !o.getStatus().equalsIgnoreCase("CANCELLED"))
                    .mapToDouble(Order::getTotalPrice)
                    .sum();

            // Outlets stats today
            Map<String, Long> outletOrdersCount = todayOrders.stream()
                    .map(o -> o.getCustomer() != null ? o.getCustomer().getName() : o.getCustomerName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

            Map<String, Double> outletRevenue = todayOrders.stream()
                    .filter(o -> o.getStatus() != null && !o.getStatus().equalsIgnoreCase("CANCELLED"))
                    .collect(Collectors.groupingBy(
                            o -> o.getCustomer() != null ? o.getCustomer().getName() : o.getCustomerName(),
                            Collectors.summingDouble(Order::getTotalPrice)
                    ));

            // Overall Status counts
            Map<String, Long> overallStatusCounts = allOrders.stream()
                    .filter(o -> o.getStatus() != null)
                    .collect(Collectors.groupingBy(o -> o.getStatus().toUpperCase(), Collectors.counting()));

            long pendingOverall = overallStatusCounts.getOrDefault("PENDING", 0L);
            long packedOverall = overallStatusCounts.getOrDefault("PACKED", 0L);
            long shippedOverall = overallStatusCounts.getOrDefault("SHIPPED", 0L);
            long deliveredOverall = overallStatusCounts.getOrDefault("DELIVERED", 0L);
            long cancelledOverall = overallStatusCounts.getOrDefault("CANCELLED", 0L);

            // Today Status counts
            Map<String, Long> todayStatusCounts = todayOrders.stream()
                    .filter(o -> o.getStatus() != null)
                    .collect(Collectors.groupingBy(o -> o.getStatus().toUpperCase(), Collectors.counting()));

            long pendingToday = todayStatusCounts.getOrDefault("PENDING", 0L);
            long packedToday = todayStatusCounts.getOrDefault("PACKED", 0L);
            long shippedToday = todayStatusCounts.getOrDefault("SHIPPED", 0L);
            long deliveredToday = todayStatusCounts.getOrDefault("DELIVERED", 0L);
            long cancelledToday = todayStatusCounts.getOrDefault("CANCELLED", 0L);

            // Build beautiful email HTML
            String htmlReport = buildHtmlReport(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                    totalOrdersToday,
                    totalRevenueToday,
                    outletOrdersCount,
                    outletRevenue,
                    pendingOverall,
                    packedOverall,
                    shippedOverall,
                    deliveredOverall,
                    cancelledOverall,
                    pendingToday,
                    packedToday,
                    shippedToday,
                    deliveredToday,
                    cancelledToday,
                    lowStockProducts
            );

            String subject = "📊 Daily Enterprise Sales & Inventory Report - " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Generate PDF Report bytes
            byte[] pdfBytes = generatePdfReport(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                    totalOrdersToday,
                    totalRevenueToday,
                    outletOrdersCount,
                    outletRevenue,
                    pendingOverall,
                    packedOverall,
                    shippedOverall,
                    deliveredOverall,
                    cancelledOverall,
                    pendingToday,
                    packedToday,
                    shippedToday,
                    deliveredToday,
                    cancelledToday,
                    lowStockProducts
            );
            
            // Send email with PDF attachment
            emailService.sendEmailWithAttachment(
                    reportToAddress,
                    subject,
                    htmlReport,
                    pdfBytes,
                    "Daily_Enterprise_Sales_Report_" + LocalDate.now().toString() + ".pdf"
            );

            System.out.println("[SalesSummaryScheduler] ✔ Daily sales summary report sent successfully to: " + reportToAddress);
        } catch (Exception e) {
            System.err.println("[SalesSummaryScheduler] ❌ Failed to generate/send daily sales summary report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Public method to generate the daily PDF on-demand for direct download (no email sent).
     */
    public byte[] generatePdfForDownload() throws Exception {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        List<Order> todayOrders = orderRepository.findByOrderDateBetweenAndDeletedFalse(startOfToday, endOfToday);
        List<Order> allOrders = orderRepository.findByDeletedFalse();
        List<Product> lowStockProducts = productRepository.findByDeletedFalseAndQuantityLessThan(lowStockThreshold);

        double totalRevenueToday = todayOrders.stream()
                .filter(o -> o.getStatus() != null && !o.getStatus().equalsIgnoreCase("CANCELLED"))
                .mapToDouble(Order::getTotalPrice).sum();

        Map<String, Long> outletOrdersCount = todayOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getName() : o.getCustomerName())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(name -> name, java.util.stream.Collectors.counting()));

        Map<String, Double> outletRevenue = todayOrders.stream()
                .filter(o -> o.getStatus() != null && !o.getStatus().equalsIgnoreCase("CANCELLED"))
                .collect(java.util.stream.Collectors.groupingBy(
                        o -> o.getCustomer() != null ? o.getCustomer().getName() : o.getCustomerName(),
                        java.util.stream.Collectors.summingDouble(Order::getTotalPrice)));

        Map<String, Long> overallStatusCounts = allOrders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(java.util.stream.Collectors.groupingBy(o -> o.getStatus().toUpperCase(), java.util.stream.Collectors.counting()));

        Map<String, Long> todayStatusCounts = todayOrders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(java.util.stream.Collectors.groupingBy(o -> o.getStatus().toUpperCase(), java.util.stream.Collectors.counting()));

        return generatePdfReport(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                todayOrders.size(),
                totalRevenueToday,
                outletOrdersCount,
                outletRevenue,
                overallStatusCounts.getOrDefault("PENDING", 0L),
                overallStatusCounts.getOrDefault("PACKED", 0L),
                overallStatusCounts.getOrDefault("SHIPPED", 0L),
                overallStatusCounts.getOrDefault("DELIVERED", 0L),
                overallStatusCounts.getOrDefault("CANCELLED", 0L),
                todayStatusCounts.getOrDefault("PENDING", 0L),
                todayStatusCounts.getOrDefault("PACKED", 0L),
                todayStatusCounts.getOrDefault("SHIPPED", 0L),
                todayStatusCounts.getOrDefault("DELIVERED", 0L),
                todayStatusCounts.getOrDefault("CANCELLED", 0L),
                lowStockProducts
        );
    }

    private String buildHtmlReport(
            String dateStr,
            int totalOrdersToday,
            double totalRevenueToday,
            Map<String, Long> outletOrdersCount,
            Map<String, Double> outletRevenue,
            long pendingOverall,
            long packedOverall,
            long shippedOverall,
            long deliveredOverall,
            long cancelledOverall,
            long pendingToday,
            long packedToday,
            long shippedToday,
            long deliveredToday,
            long cancelledToday,
            List<Product> lowStockProducts
    ) {
        StringBuilder outletsRows = new StringBuilder();
        if (outletOrdersCount.isEmpty()) {
            outletsRows.append("<tr><td colspan='3' style='text-align:center; padding: 12px; color: #64748b; font-style: italic; font-size: 13px;'>No outlets placed orders today.</td></tr>");
        } else {
            List<String> sortedOutlets = new ArrayList<>(outletOrdersCount.keySet());
            sortedOutlets.sort((a, b) -> Double.compare(outletRevenue.getOrDefault(b, 0.0), outletRevenue.getOrDefault(a, 0.0)));
            for (String outlet : sortedOutlets) {
                long count = outletOrdersCount.getOrDefault(outlet, 0L);
                double revenue = outletRevenue.getOrDefault(outlet, 0.0);
                outletsRows.append(String.format(
                        "<tr>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #334155;'>%s</td>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: center; font-size: 13px; color: #334155;'>%d</td>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: right; font-size: 13px; font-weight: 700; color: #0f172a;'>₹%,.2f</td>" +
                        "</tr>",
                        outlet, count, revenue
                ));
            }
        }

        StringBuilder lowStockRows = new StringBuilder();
        if (lowStockProducts.isEmpty()) {
            lowStockRows.append("<tr><td colspan='4' style='text-align:center; padding: 12px; color: #059669; font-weight: 600; font-size: 13px;'>✔ All products are well stocked.</td></tr>");
        } else {
            for (Product p : lowStockProducts) {
                lowStockRows.append(String.format(
                        "<tr>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #b91c1c; font-weight: 600;'>%s</td>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #334155;'>%s</td>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #334155;'>%s</td>" +
                        "<td style='padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: center; font-size: 13px; font-weight: 700; color: #b91c1c;'>%d</td>" +
                        "</tr>",
                        p.getName(), p.getCode(), p.getSku() != null ? p.getSku() : "N/A", p.getQuantity()
                ));
            }
        }

        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Daily Enterprise Sales & Inventory Report</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background-color: #ffffff;
                        color: #334155;
                        margin: 0;
                        padding: 0;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 650px;
                        margin: 30px auto;
                        background-color: #ffffff;
                        border-radius: 6px;
                        overflow: hidden;
                        border: 1px solid #e2e8f0;
                    }
                    .header {
                        background-color: #ffffff;
                        padding: 24px 30px;
                        border-bottom: 1px solid #e2e8f0;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 20px;
                        font-weight: 700;
                        color: #1e3a8a;
                    }
                    .header p {
                        margin: 6px 0 0 0;
                        font-size: 13px;
                        color: #64748b;
                    }
                    .content {
                        padding: 24px 30px;
                    }
                    .metrics-table {
                        width: 100%%;
                        border-collapse: separate;
                        border-spacing: 12px;
                        margin-bottom: 16px;
                    }
                    .metric-card {
                        padding: 12px;
                        border-radius: 4px;
                        background-color: #ffffff;
                        border: 1px solid #e2e8f0;
                        text-align: center;
                    }
                    .metric-card .title {
                        font-size: 11px;
                        text-transform: uppercase;
                        font-weight: 700;
                        letter-spacing: 0.5px;
                        color: #64748b;
                        margin-bottom: 4px;
                    }
                    .metric-card .val {
                        font-size: 18px;
                        font-weight: 700;
                        color: #0f172a;
                    }
                    .section-title {
                        font-size: 14px;
                        font-weight: 700;
                        color: #1e3a8a;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        padding-bottom: 8px;
                        border-bottom: 1px solid #e2e8f0;
                        margin: 24px 0 12px 0;
                    }
                    table.data-table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                    }
                    th {
                        background-color: #f8fafc;
                        color: #475569;
                        font-weight: 700;
                        font-size: 12px;
                        text-transform: uppercase;
                        padding: 10px 12px;
                        text-align: left;
                        border-bottom: 1px solid #e2e8f0;
                    }
                    .status-table td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #f1f5f9;
                        font-size: 13px;
                    }
                    .status-badge {
                        font-size: 13px;
                        font-weight: 600;
                        color: #475569;
                    }
                    .badge-pending { color: #d97706; }
                    .badge-packed { color: #64748b; }
                    .badge-shipped { color: #2563eb; }
                    .badge-delivered { color: #059669; }
                    .badge-cancelled { color: #dc2626; }
                    .footer {
                        background-color: #f8fafc;
                        padding: 16px 30px;
                        text-align: center;
                        font-size: 11px;
                        color: #64748b;
                        border-top: 1px solid #e2e8f0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Daily Enterprise Report</h1>
                        <p>REPORT_DATE</p>
                    </div>
                    <div class="content">
                        <!-- Key Metrics -->
                        <table class="metrics-table" style="width: 100%%; border: none; border-collapse: separate; border-spacing: 10px; margin-bottom: 20px;">
                            <tr>
                                <td style="width: 25%%; padding: 0;">
                                    <div class="metric-card">
                                        <div class="title">Sales Today</div>
                                        <div class="val">₹%,.2f</div>
                                    </div>
                                </td>
                                <td style="width: 25%%; padding: 0;">
                                    <div class="metric-card">
                                        <div class="title">Active Outlets</div>
                                        <div class="val">%d</div>
                                    </div>
                                </td>
                                <td style="width: 25%%; padding: 0;">
                                    <div class="metric-card">
                                        <div class="title">Pending Orders</div>
                                        <div class="val">%d</div>
                                    </div>
                                </td>
                                <td style="width: 25%%; padding: 0;">
                                    <div class="metric-card">
                                        <div class="title">Low Stock Items</div>
                                        <div class="val">%d</div>
                                    </div>
                                </td>
                            </tr>
                        </table>
 
                        <!-- Today vs Overall Order Status -->
                        <div class="section-title">Order Status Summary</div>
                        <table class="data-table status-table">
                            <thead>
                                <tr>
                                    <th>Status</th>
                                    <th style="text-align: center;">Placed Today</th>
                                    <th style="text-align: center;">Overall Open</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><span class="status-badge badge-pending">Pending</span></td>
                                    <td style="text-align: center; font-weight: 500;">%d</td>
                                    <td style="text-align: center; font-weight: 700; color: #d97706;">%d</td>
                                </tr>
                                <tr>
                                    <td><span class="status-badge badge-packed">Packed</span></td>
                                    <td style="text-align: center; font-weight: 500;">%d</td>
                                    <td style="text-align: center; font-weight: 500;">%d</td>
                                </tr>
                                <tr>
                                    <td><span class="status-badge badge-shipped">Shipped</span></td>
                                    <td style="text-align: center; font-weight: 500;">%d</td>
                                    <td style="text-align: center; font-weight: 500;">%d</td>
                                </tr>
                                <tr>
                                    <td><span class="status-badge badge-delivered">Delivered</span></td>
                                    <td style="text-align: center; font-weight: 500; color: #059669;">%d</td>
                                    <td style="text-align: center; font-weight: 500; color: #059669;">%d</td>
                                </tr>
                                <tr>
                                    <td><span class="status-badge badge-cancelled">Cancelled</span></td>
                                    <td style="text-align: center; font-weight: 500; color: #dc2626;">%d</td>
                                    <td style="text-align: center; font-weight: 500; color: #dc2626;">%d</td>
                                </tr>
                            </tbody>
                        </table>
 
                        <!-- Outlet Breakdown -->
                        <div class="section-title">Outlets Order Breakdown</div>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Outlet / Customer Name</th>
                                    <th style="text-align: center; width: 100px;">Orders Count</th>
                                    <th style="text-align: right; width: 150px;">Total Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                OUTLETS_ROWS
                            </tbody>
                        </table>
 
                        <!-- Stock Alerts -->
                        <div class="section-title">Low Stock Alerts (Threshold: LOW_STOCK_THRESHOLD units)</div>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Product Name</th>
                                    <th>Product Code</th>
                                    <th>SKU</th>
                                    <th style="text-align: center; width: 80px;">Qty Left</th>
                                </tr>
                            </thead>
                            <tbody>
                                LOW_STOCK_ROWS
                            </tbody>
                        </table>
                        <!-- Download Button -->
                        <div style="margin: 24px 0 10px 0; text-align: center;">
                            <a href="API_BASE_URL/api/v1/email/download-sales-report" style="display: inline-block; padding: 10px 20px; background: #1e3a8a; color: #ffffff; text-decoration: none; font-weight: 700; border-radius: 4px; font-size: 13px; text-align: center;">Download PDF Report</a>
                        </div>
                    </div>
                    <div class="footer">
                        This is an automatically generated system report from Product Inventory Management.
                    </div>
                </div>
            </body>
            </html>
            """;

        return template
                .replace("REPORT_DATE", dateStr)
                .replace("OUTLETS_ROWS", outletsRows.toString())
                .replace("LOW_STOCK_ROWS", lowStockRows.toString())
                .replace("LOW_STOCK_THRESHOLD", String.valueOf(lowStockThreshold))
                .replace("API_BASE_URL", apiBaseUrl != null ? apiBaseUrl : "http://localhost:8080")
                .formatted(
                        totalRevenueToday,
                        outletOrdersCount.size(),
                        pendingOverall,
                        lowStockProducts.size(),
                        pendingToday, pendingOverall,
                        packedToday, packedOverall,
                        shippedToday, shippedOverall,
                        deliveredToday, deliveredOverall,
                        cancelledToday, cancelledOverall
                );
    }

    private byte[] generatePdfReport(
            String dateStr,
            int totalOrdersToday,
            double totalRevenueToday,
            Map<String, Long> outletOrdersCount,
            Map<String, Double> outletRevenue,
            long pendingOverall,
            long packedOverall,
            long shippedOverall,
            long deliveredOverall,
            long cancelledOverall,
            long pendingToday,
            long packedToday,
            long shippedToday,
            long deliveredToday,
            long cancelledToday,
            List<Product> lowStockProducts
    ) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(30, 30, 30, 30);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            DeviceRgb primaryColor = new DeviceRgb(30, 58, 138); // Dark Blue #1e3a8a
            DeviceRgb secondaryColor = new DeviceRgb(59, 130, 246); // Blue #3b82f6
            DeviceRgb grayBg = new DeviceRgb(247, 250, 252);
            DeviceRgb borderGray = new DeviceRgb(226, 232, 240);

            // Header/Title
            doc.add(new Paragraph("Daily Enterprise Report")
                    .setFont(boldFont).setFontSize(22)
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2));

            doc.add(new Paragraph(dateStr)
                    .setFont(regularFont).setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15));

            // Metrics Grid (4 columns)
            float[] metricsWidths = {120f, 120f, 120f, 120f};
            Table metricsTable = new Table(UnitValue.createPointArray(metricsWidths));
            metricsTable.setWidth(UnitValue.createPercentValue(100));
            metricsTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            metricsTable.setMarginBottom(20);

            // Add Metric Cards
            metricsTable.addCell(createMetricCell("Sales Today", String.format("Rs. %,.2f", totalRevenueToday), new DeviceRgb(49, 130, 206), boldFont, regularFont));
            metricsTable.addCell(createMetricCell("Active Outlets", String.valueOf(outletOrdersCount.size()), new DeviceRgb(56, 161, 105), boldFont, regularFont));
            metricsTable.addCell(createMetricCell("Pending Orders", String.valueOf(pendingOverall), new DeviceRgb(221, 107, 32), boldFont, regularFont));
            metricsTable.addCell(createMetricCell("Low Stock Items", String.valueOf(lowStockProducts.size()), new DeviceRgb(229, 62, 62), boldFont, regularFont));
            doc.add(metricsTable);

            // Section: Order Status Summary
            doc.add(new Paragraph("Order Status Summary")
                    .setFont(boldFont).setFontSize(14)
                    .setFontColor(primaryColor)
                    .setMarginBottom(8));

            float[] statusWidths = {180f, 150f, 150f};
            Table statusTable = new Table(UnitValue.createPointArray(statusWidths));
            statusTable.setWidth(UnitValue.createPercentValue(100));
            statusTable.setMarginBottom(20);

            // Header
            statusTable.addHeaderCell(createHeaderCell("Status", boldFont, primaryColor));
            statusTable.addHeaderCell(createHeaderCell("Placed Today", boldFont, primaryColor));
            statusTable.addHeaderCell(createHeaderCell("Overall Open", boldFont, primaryColor));

            // Rows
            addStatusRow(statusTable, "Pending", pendingToday, pendingOverall, regularFont, boldFont);
            addStatusRow(statusTable, "Packed", packedToday, packedOverall, regularFont, boldFont);
            addStatusRow(statusTable, "Shipped", shippedToday, shippedOverall, regularFont, boldFont);
            addStatusRow(statusTable, "Delivered", deliveredToday, deliveredOverall, regularFont, boldFont);
            addStatusRow(statusTable, "Cancelled", cancelledToday, cancelledOverall, regularFont, boldFont);
            doc.add(statusTable);

            // Section: Outlets Order Breakdown
            doc.add(new Paragraph("Outlets Order Breakdown")
                    .setFont(boldFont).setFontSize(14)
                    .setFontColor(primaryColor)
                    .setMarginBottom(8));

            float[] outletWidths = {220f, 110f, 150f};
            Table outletTable = new Table(UnitValue.createPointArray(outletWidths));
            outletTable.setWidth(UnitValue.createPercentValue(100));
            outletTable.setMarginBottom(20);

            outletTable.addHeaderCell(createHeaderCell("Outlet / Customer Name", boldFont, primaryColor));
            outletTable.addHeaderCell(createHeaderCell("Orders Count", boldFont, primaryColor));
            outletTable.addHeaderCell(createHeaderCell("Total Value", boldFont, primaryColor));

            if (outletOrdersCount.isEmpty()) {
                outletTable.addCell(new Cell(1, 3)
                        .add(new Paragraph("No outlets placed orders today.")
                                .setFont(regularFont).setFontSize(10).setItalic()
                                .setTextAlignment(TextAlignment.CENTER))
                        .setPadding(10));
            } else {
                List<String> sortedOutlets = new ArrayList<>(outletOrdersCount.keySet());
                sortedOutlets.sort((a, b) -> Double.compare(outletRevenue.getOrDefault(b, 0.0), outletRevenue.getOrDefault(a, 0.0)));
                for (String outlet : sortedOutlets) {
                    long count = outletOrdersCount.getOrDefault(outlet, 0L);
                    double revenue = outletRevenue.getOrDefault(outlet, 0.0);
                    outletTable.addCell(new Cell().add(new Paragraph(outlet).setFont(regularFont).setFontSize(10)).setPadding(8));
                    outletTable.addCell(new Cell().add(new Paragraph(String.valueOf(count)).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER)).setPadding(8));
                    outletTable.addCell(new Cell().add(new Paragraph(String.format("Rs. %,.2f", revenue)).setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFontColor(new DeviceRgb(49, 130, 206))).setPadding(8));
                }
            }
            doc.add(outletTable);

            // Section: Low Stock Alerts
            doc.add(new Paragraph("Low Stock Alerts (Threshold: " + lowStockThreshold + " units)")
                    .setFont(boldFont).setFontSize(14)
                    .setFontColor(primaryColor)
                    .setMarginBottom(8));

            float[] stockWidths = {160f, 110f, 110f, 100f};
            Table stockTable = new Table(UnitValue.createPointArray(stockWidths));
            stockTable.setWidth(UnitValue.createPercentValue(100));
            stockTable.setMarginBottom(10);

            stockTable.addHeaderCell(createHeaderCell("Product Name", boldFont, primaryColor));
            stockTable.addHeaderCell(createHeaderCell("Product Code", boldFont, primaryColor));
            stockTable.addHeaderCell(createHeaderCell("SKU", boldFont, primaryColor));
            stockTable.addHeaderCell(createHeaderCell("Qty Left", boldFont, primaryColor));

            if (lowStockProducts.isEmpty()) {
                stockTable.addCell(new Cell(1, 4)
                        .add(new Paragraph("All products are well stocked.")
                                .setFont(regularFont).setFontSize(10)
                                .setTextAlignment(TextAlignment.CENTER))
                        .setPadding(10));
            } else {
                for (Product p : lowStockProducts) {
                    stockTable.addCell(new Cell().add(new Paragraph(p.getName()).setFont(regularFont).setFontSize(10)).setPadding(8));
                    stockTable.addCell(new Cell().add(new Paragraph(p.getCode()).setFont(regularFont).setFontSize(10)).setPadding(8));
                    stockTable.addCell(new Cell().add(new Paragraph(p.getSku() != null ? p.getSku() : "N/A").setFont(regularFont).setFontSize(10)).setPadding(8));
                    stockTable.addCell(new Cell().add(new Paragraph(String.valueOf(p.getQuantity())).setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.RED)).setPadding(8));
                }
            }
            doc.add(stockTable);

            // Footer
            doc.add(new Paragraph("\nProduct Inventory Management System  —  Confidential")
                    .setFont(regularFont).setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(15));
        }

        return out.toByteArray();
    }

    private Cell createMetricCell(String title, String val, DeviceRgb bg, PdfFont bold, PdfFont reg) {
        Cell cell = new Cell();
        cell.setBackgroundColor(bg);
        cell.setPadding(10);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.add(new Paragraph(title.toUpperCase())
                .setFont(bold).setFontSize(8)
                .setFontColor(ColorConstants.WHITE)
                .setMarginBottom(2));
        cell.add(new Paragraph(val)
                .setFont(bold).setFontSize(14)
                .setFontColor(ColorConstants.WHITE));
        return cell;
    }

    private Cell createHeaderCell(String text, PdfFont font, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(bg)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private void addStatusRow(Table table, String status, long today, long overall, PdfFont reg, PdfFont bold) {
        table.addCell(new Cell().add(new Paragraph(status).setFont(reg).setFontSize(10)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(today)).setFont(reg).setFontSize(10).setTextAlignment(TextAlignment.CENTER)).setPadding(8));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(overall)).setFont(bold).setFontSize(10).setTextAlignment(TextAlignment.CENTER)).setPadding(8));
    }
}
