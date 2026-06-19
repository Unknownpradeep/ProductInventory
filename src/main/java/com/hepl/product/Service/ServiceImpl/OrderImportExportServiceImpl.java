package com.hepl.product.Service.ServiceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.hepl.product.model.Order;
import com.hepl.product.Service.OrderImportExportService;
import com.hepl.product.Util.ExportUtils;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderImportExportServiceImpl implements OrderImportExportService {

    private final com.hepl.product.Service.OrderService orderService;
    private final com.hepl.product.Repository.OrderRepository orderRepository;
    private final com.hepl.product.Repository.CustomerRepository customerRepository;
    private final com.hepl.product.Repository.ProductRepository productRepository;
    private final com.hepl.product.Service.SocketIOService socketIOService;

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        com.hepl.product.Service.SocketIOService.setNotificationSuppression(true);
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            int success = 0, failed = 0;

            // Helper class to store valid row details for Pass 2
            class ValidRow {
            int rowNum;
            com.hepl.product.model.Customer customer;
            com.hepl.product.model.Product product;
            int quantity;
            double discount;
            double gst;
            String status;
            String paymentStatus;

            ValidRow(int rowNum, com.hepl.product.model.Customer customer, com.hepl.product.model.Product product,
                    int quantity, double discount, double gst, String status, String paymentStatus) {
                this.rowNum = rowNum;
                this.customer = customer;
                this.product = product;
                this.quantity = quantity;
                this.discount = discount;
                this.gst = gst;
                this.status = status;
                this.paymentStatus = paymentStatus;
            }
        }

        List<ValidRow> validRows = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();

            // Skip the title row (row 0) and header row (row 1)
            if (rowIter.hasNext())
                rowIter.next(); // skip row 0
            if (rowIter.hasNext())
                rowIter.next(); // skip row 1

            int rowNum = 3;
            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                String customerName = ExportUtils.getCellStr(row, 0).trim();
                String productName = ExportUtils.getCellStr(row, 1).trim();
                String qtyStr = ExportUtils.getCellStr(row, 2).trim();
                String discountStr = ExportUtils.getCellStr(row, 3).trim();
                String gstStr = ExportUtils.getCellStr(row, 4).trim();
                String status = ExportUtils.getCellStr(row, 5).trim().toUpperCase();
                String paymentStatus = ExportUtils.getCellStr(row, 6).trim().toUpperCase();

                if (customerName.isBlank() && productName.isBlank()) {
                    continue; // Skip empty rows
                }

                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", rowNum++);

                List<String> errors = new ArrayList<>();

                // Validate Customer
                com.hepl.product.model.Customer customer = null;
                if (customerName.isBlank()) {
                    errors.add("Customer Name is required");
                } else {
                    List<com.hepl.product.model.Customer> custs = customerRepository.findAllByNameIgnoreCase(customerName);
                    com.hepl.product.model.Customer foundCust = custs.stream()
                            .filter(c -> !c.isDeleted())
                            .findFirst()
                            .orElse(custs.isEmpty() ? null : custs.get(0));
                    if (foundCust == null) {
                        errors.add("Customer not found: " + customerName);
                    } else {
                        customer = foundCust;
                    }
                }

                // Validate Product
                com.hepl.product.model.Product product = null;
                if (productName.isBlank()) {
                    errors.add("Product Name is required");
                } else {
                    List<com.hepl.product.model.Product> prods = productRepository.findAllByNameIgnoreCase(productName);
                    com.hepl.product.model.Product foundProd = prods.stream()
                            .filter(p -> !p.isDeleted())
                            .findFirst()
                            .orElse(prods.isEmpty() ? null : prods.get(0));
                    if (foundProd == null) {
                        errors.add("Product not found: " + productName);
                    } else {
                        product = foundProd;
                    }
                }

                // Validate Quantity
                int quantity = 0;
                if (qtyStr.isBlank()) {
                    errors.add("Quantity is required");
                } else {
                    try {
                        quantity = (int) Double.parseDouble(qtyStr);
                        if (quantity <= 0) {
                            errors.add("Quantity must be greater than 0");
                        }
                    } catch (Exception e) {
                        errors.add("Invalid quantity format");
                    }
                }

                // Validate Discount
                double discount = 0;
                if (!discountStr.isBlank()) {
                    try {
                        discount = Double.parseDouble(discountStr);
                        if (discount < 0 || discount > 100) {
                            errors.add("Discount % must be between 0 and 100");
                        }
                    } catch (Exception e) {
                        errors.add("Invalid discount format");
                    }
                }

                // Validate GST
                double gst = 0;
                if (!gstStr.isBlank()) {
                    try {
                        gst = Double.parseDouble(gstStr);
                        if (gst < 0 || gst > 100) {
                            errors.add("GST % must be between 0 and 100");
                        }
                    } catch (Exception e) {
                        errors.add("Invalid GST format");
                    }
                }

                // Validate Status
                List<String> validStatuses = List.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED");
                if (status.isBlank()) {
                    errors.add("Status is required");
                } else if (!validStatuses.contains(status)) {
                    errors.add("Status must be PENDING, SHIPPED, DELIVERED, or CANCELLED");
                }

                // Validate Payment Status
                List<String> validPayments = List.of("PENDING", "SUCCESS", "FAILED");
                if (paymentStatus.isBlank()) {
                    errors.add("Payment Status is required");
                } else if (!validPayments.contains(paymentStatus)) {
                    errors.add("Payment Status must be PENDING, SUCCESS, or FAILED");
                }

                if (!errors.isEmpty()) {
                    rowResult.put("status", "FAILED");
                    rowResult.put("errors", errors);
                    results.add(rowResult);
                    failed++;
                } else {
                    validRows.add(new ValidRow(rowNum - 1, customer, product, quantity, discount, gst, status,
                            paymentStatus));
                }
            }
        }

        // Pass 2: Group consecutive valid rows and save them
        if (!validRows.isEmpty()) {
            com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto currentDto = null;
            List<ValidRow> currentGroup = new ArrayList<>();

            for (int i = 0; i < validRows.size(); i++) {
                ValidRow vr = validRows.get(i);
                boolean isNewGroup = (currentDto == null) ||
                        (!currentDto.getCustomerId().equals(vr.customer.getId())) ||
                        (!currentDto.getStatus().equals(vr.status)) ||
                        (!currentDto.getPaymentStatus().equals(vr.paymentStatus));
                if (isNewGroup) {
                    if (currentDto != null) {
                        // Save current group
                        try {
                            if (checkIfOrderExists(currentDto)) {
                                for (ValidRow savedVr : currentGroup) {
                                    Map<String, Object> rowResult = new LinkedHashMap<>();
                                    rowResult.put("row", savedVr.rowNum);
                                    rowResult.put("status", "SUCCESS");
                                    rowResult.put("customer", savedVr.customer.getName());
                                    rowResult.put("product", savedVr.product.getName());
                                    results.add(rowResult);
                                    success++;
                                }
                            } else {
                                orderService.save(currentDto);
                                for (ValidRow savedVr : currentGroup) {
                                    Map<String, Object> rowResult = new LinkedHashMap<>();
                                    rowResult.put("row", savedVr.rowNum);
                                    rowResult.put("status", "SUCCESS");
                                    rowResult.put("customer", savedVr.customer.getName());
                                    rowResult.put("product", savedVr.product.getName());
                                    results.add(rowResult);
                                    success++;
                                }
                            }
                        } catch (Exception e) {
                            for (ValidRow savedVr : currentGroup) {
                                Map<String, Object> rowResult = new LinkedHashMap<>();
                                rowResult.put("row", savedVr.rowNum);
                                rowResult.put("status", "FAILED");
                                rowResult.put("errors", List.of("Database save error: " + e.getMessage()));
                                results.add(rowResult);
                                failed++;
                            }
                        }
                    }

                    // Start new group
                    currentDto = new com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto();
                    currentDto.setCustomerId(vr.customer.getId());
                    currentDto.setCustomerName(vr.customer.getName());
                    currentDto.setStatus(vr.status);
                    currentDto.setPaymentStatus(vr.paymentStatus);
                    currentDto.setOrderItems(new ArrayList<>());
                    currentGroup = new ArrayList<>();
                }

                com.hepl.product.Payload.Dto.OrderDto.OrderItemDto itemDto = new com.hepl.product.Payload.Dto.OrderDto.OrderItemDto();
                itemDto.setProductId(vr.product.getId());
                itemDto.setQuantity(vr.quantity);
                itemDto.setDiscount(vr.discount);
                itemDto.setGstpercentage(vr.gst);
                currentDto.getOrderItems().add(itemDto);
                currentGroup.add(vr);
            }

            // Save the last group
            if (currentDto != null) {
                try {
                    if (checkIfOrderExists(currentDto)) {
                        for (ValidRow savedVr : currentGroup) {
                            Map<String, Object> rowResult = new LinkedHashMap<>();
                            rowResult.put("row", savedVr.rowNum);
                            rowResult.put("status", "SUCCESS");
                            rowResult.put("customer", savedVr.customer.getName());
                            rowResult.put("product", savedVr.product.getName());
                            results.add(rowResult);
                            success++;
                        }
                    } else {
                        orderService.save(currentDto);
                        for (ValidRow savedVr : currentGroup) {
                            Map<String, Object> rowResult = new LinkedHashMap<>();
                            rowResult.put("row", savedVr.rowNum);
                            rowResult.put("status", "SUCCESS");
                            rowResult.put("customer", savedVr.customer.getName());
                            rowResult.put("product", savedVr.product.getName());
                            results.add(rowResult);
                            success++;
                        }
                    }
                } catch (Exception e) {
                    for (ValidRow savedVr : currentGroup) {
                        Map<String, Object> rowResult = new LinkedHashMap<>();
                        rowResult.put("row", savedVr.rowNum);
                        rowResult.put("status", "FAILED");
                        rowResult.put("errors", List.of("Database save error: " + e.getMessage()));
                        results.add(rowResult);
                        failed++;
                    }
                }
            }
        }

        // Sort results by row number to keep it perfectly aligned
        results.sort(Comparator.comparingInt(m -> (Integer) m.get("row")));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalProcessed", success + failed);
        resp.put("success", success);
        resp.put("failed", failed);
        resp.put("rows", results);

        resp.put("totalRows", success + failed);
        resp.put("successCount", success);
        resp.put("failedCount", failed);
        List<Map<String, Object>> failedRowsList = new ArrayList<>();
        for (Map<String, Object> r : results) {
            if ("FAILED".equals(r.get("status"))) {
                failedRowsList.add(r);
            }
        }
        resp.put("failedRows", failedRowsList);

        if (success > 0) {
            com.hepl.product.Service.SocketIOService.setNotificationSuppression(false);
            try {
                socketIOService.emitNotification("Bulk Import Success", 
                        "Successfully imported " + success + " orders from Excel sheet.", "success");
            } catch (Exception e) {
                System.err.println("Failed to emit bulk import notification: " + e.getMessage());
            }
        }

        return resp;
        } finally {
            com.hepl.product.Service.SocketIOService.clearNotificationSuppression();
        }
    }

    @Override
    public byte[] exportToExcel(List<Order> orders) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Orders");
            sheet.setDefaultColumnWidth(20);

            String[] exportHeaders = { "Customer Name*", "Product Name*", "Quantity*", "Discount %*", "GST %*",
                    "Status (PENDING/SHIPPED/DELIVERED/CANCELLED)*", "Payment Status (PENDING/SUCCESS/FAILED)*" };

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            ExportUtils.setPoiCell(titleRow, 0, "Order History Export | " + LocalDate.now(),
                    ExportUtils.createTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, exportHeaders.length - 1));

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < exportHeaders.length; i++) {
                ExportUtils.setPoiCell(headerRow, i, exportHeaders[i], hStyle);
            }

            CellStyle even = ExportUtils.createEvenRowStyle(wb);
            CellStyle odd = ExportUtils.createOddRowStyle(wb);
            int rowIndex = 2;
            int serial = 1;
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                if (o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
                    for (com.hepl.product.model.OrderItem item : o.getOrderItems()) {
                        Row row = sheet.createRow(rowIndex++);
                        CellStyle style = (serial % 2 == 0) ? even : odd;
                        ExportUtils.setPoiCell(row, 0, o.getCustomerName() != null ? o.getCustomerName()
                                : (o.getCustomer() != null ? o.getCustomer().getName() : "-"), style);
                        ExportUtils.setPoiCell(row, 1, item.getProductName() != null ? item.getProductName()
                                : (item.getProduct() != null ? item.getProduct().getName() : "-"), style);
                        ExportUtils.setPoiCell(row, 2, String.valueOf(item.getQuantity()), style);
                        ExportUtils.setPoiCell(row, 3, String.valueOf(item.getDiscount()), style);
                        ExportUtils.setPoiCell(row, 4, String.valueOf(item.getGstpercentage()), style);
                        ExportUtils.setPoiCell(row, 5, o.getStatus() != null ? o.getStatus() : "PENDING", style);
                        ExportUtils.setPoiCell(row, 6, o.getPaymentstatus() != null ? o.getPaymentstatus() : "PENDING",
                                style);
                        serial++;
                    }
                } else {
                    Row row = sheet.createRow(rowIndex++);
                    CellStyle style = (serial % 2 == 0) ? even : odd;
                    ExportUtils.setPoiCell(row, 0, o.getCustomerName() != null ? o.getCustomerName()
                            : (o.getCustomer() != null ? o.getCustomer().getName() : "-"), style);
                    ExportUtils.setPoiCell(row, 1, "-", style);
                    ExportUtils.setPoiCell(row, 2, "0", style);
                    ExportUtils.setPoiCell(row, 3, "0", style);
                    ExportUtils.setPoiCell(row, 4, "0", style);
                    ExportUtils.setPoiCell(row, 5, o.getStatus() != null ? o.getStatus() : "PENDING", style);
                    ExportUtils.setPoiCell(row, 6, o.getPaymentstatus() != null ? o.getPaymentstatus() : "PENDING",
                            style);
                    serial++;
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<Order> orders) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            ExportUtils.addPdfTitle(doc, "Order History Report", orders.size(), bold, reg);

            float[] widths = { 40f, 100f, 150f, 80f, 80f, 80f, 80f };
            Table table = new Table(UnitValue.createPointArray(widths)).useAllAvailableWidth();
            DeviceRgb hBg = new DeviceRgb(99, 102, 241);
            String[] cols = { "#", "Code", "Customer", "Amount", "Status", "Payment", "Date" };
            for (String col : cols)
                table.addHeaderCell(ExportUtils.pdfHeaderCell(col, bold, hBg));

            DeviceRgb evenBg = new DeviceRgb(248, 249, 255);
            DeviceRgb white = new DeviceRgb(255, 255, 255);
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                DeviceRgb bg = (i % 2 == 0) ? evenBg : white;
                table.addCell(ExportUtils.pdfDataCell(String.valueOf(i + 1), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(o.getOrderCode(), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(o.getCustomerName() != null ? o.getCustomerName()
                        : (o.getCustomer() != null ? o.getCustomer().getName() : "-"), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell("₹" + o.getTotalPrice(), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(o.getStatus(), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(o.getPaymentstatus(), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(
                        o.getOrderDate() != null ? o.getOrderDate().toLocalDate().toString() : "-", reg, bg, true));
            }
            doc.add(table);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Order Template");
            sheet.setDefaultColumnWidth(25);

            String[] headers = { "Customer Name*", "Product Name*", "Quantity*", "Discount %*", "GST %*",
                    "Status (PENDING/SHIPPED/DELIVERED/CANCELLED)*", "Payment Status (PENDING/SUCCESS/FAILED)*" };
            String[] sample = { "John Doe", "Product Alpha", "5", "10", "18", "PENDING", "PENDING" };

            Row info = sheet.createRow(0);
            ExportUtils.setPoiCell(info, 0, "Fill in order data. Columns marked * are required. Consecutive rows with same Customer, Status, and Payment Status will be grouped into a single order.",
                    ExportUtils.createInfoStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

            Row hRow = sheet.createRow(1);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < headers.length; i++)
                ExportUtils.setPoiCell(hRow, i, headers[i], hStyle);

            Row sRow = sheet.createRow(2);
            CellStyle sStyle = ExportUtils.createSampleStyle(wb);
            for (int i = 0; i < sample.length; i++)
                ExportUtils.setPoiCell(sRow, i, sample[i], sStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private boolean checkIfOrderExists(com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto dto) {
        List<com.hepl.product.model.Order> existingOrders = orderRepository.findByCustomerId(dto.getCustomerId());
        if (existingOrders == null || existingOrders.isEmpty()) {
            return false;
        }

        for (com.hepl.product.model.Order order : existingOrders) {
            if (order.isDeleted()) {
                continue;
            }

            // Check basic fields
            if (!Objects.equals(order.getStatus(), dto.getStatus()) ||
                !Objects.equals(order.getPaymentstatus(), dto.getPaymentStatus())) {
                continue;
            }

            // Check if order items size matches
            List<com.hepl.product.model.OrderItem> dbItems = order.getOrderItems();
            List<com.hepl.product.Payload.Dto.OrderDto.OrderItemDto> importItems = dto.getOrderItems();
            if (dbItems == null || importItems == null || dbItems.size() != importItems.size()) {
                continue;
            }

            // Check if all import items match exact items in database
            boolean allItemsMatch = true;
            for (com.hepl.product.Payload.Dto.OrderDto.OrderItemDto importItem : importItems) {
                boolean itemMatched = false;
                for (com.hepl.product.model.OrderItem dbItem : dbItems) {
                    if (Objects.equals(dbItem.getProduct().getId(), importItem.getProductId()) &&
                        dbItem.getQuantity() == importItem.getQuantity() &&
                        Double.compare(dbItem.getDiscount(), importItem.getDiscount()) == 0 &&
                        Double.compare(dbItem.getGstpercentage(), importItem.getGstpercentage()) == 0) {
                        itemMatched = true;
                        break;
                    }
                }
                if (!itemMatched) {
                    allItemsMatch = false;
                    break;
                }
            }

            if (allItemsMatch) {
                return true; // Found an exact matching active order!
            }
        }

        return false;
    }
}
