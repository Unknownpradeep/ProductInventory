package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto;
import com.hepl.product.Repository.InvoiceRepository;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Service.InvoiceService;
import com.hepl.product.model.Invoice;
import com.hepl.product.model.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    @Override
    public InvoiceResponseDto generateInvoice(String orderCode) {

        // Check if invoice already exists for this order
        if (invoiceRepository.existsByOrderCode(orderCode)) {
            // Return existing invoice (must be unique per order)
            return mapToDto(invoiceRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Invoice not found")));
        }

        // Find the order
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderCode);
        }

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode("INV-" + orderCode);
        invoice.setOrder(order);
        invoice.setOrderCode(orderCode);
        invoice.setCustomer(order.getCustomer());
        invoice.setCustomerName(order.getCustomerName());
        invoice.setCustomerEmail(order.getCustomerEmail());
        invoice.setCustomerAddress(order.getCustomer() != null ? order.getCustomer().getAddress() : null);
        invoice.setBaseTotal(order.getBaseTotal() != null ? order.getBaseTotal() : 0.0);
        invoice.setTotalDiscount(order.getTotalDiscount() != null ? order.getTotalDiscount() : 0.0);
        invoice.setTotalTax(order.getTotalTax() != null ? order.getTotalTax() : 0.0);
        invoice.setFinalAmount(order.getTotalPrice());
        invoice.setStatus("PENDING");
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());

        return mapToDto(invoiceRepository.save(invoice));
    }

    @Override
    @Cacheable(value = "invoices", key = "#id")
    public InvoiceResponseDto get(Long id) {
        return mapToDto(invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice Not Found")));
    }

    @Override
    @Cacheable(value = "invoices", key = "#orderCode")
    public InvoiceResponseDto getByOrderCode(String orderCode) {
        return mapToDto(invoiceRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order: " + orderCode)));
    }

    @Override
    @CacheEvict(value = "invoices", allEntries = true)
    public InvoiceResponseDto updateStatus(Long id, String status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice Not Found"));

        // If status is APPROVED, check for duplicate
        if ("APPROVED".equalsIgnoreCase(status)) {
            if ("APPROVED".equalsIgnoreCase(invoice.getStatus())) {
                throw new RuntimeException("Invoice is already APPROVED");
            }
        }

        invoice.setStatus(status.toUpperCase());
        invoice.setUpdatedAt(LocalDateTime.now());
        return mapToDto(invoiceRepository.save(invoice));
    }

    @Override
    public Page<InvoiceResponseDto> listAll(String search, String status, Long customerId, int page, int size,
            String sortBy, String sortDir) {
        // Automatically sync all missing invoices for existing orders
        try {
            java.util.List<com.hepl.product.model.Order> allOrders = orderRepository.findAll();
            for (com.hepl.product.model.Order order : allOrders) {
                if (order.getOrderCode() != null && !order.getOrderCode().isBlank() && !"CANCELLED".equalsIgnoreCase(order.getStatus())) {
                    if (!invoiceRepository.existsByOrderCode(order.getOrderCode())) {
                        try {
                            com.hepl.product.model.Invoice invoice = new com.hepl.product.model.Invoice();
                            invoice.setInvoiceCode("INV-" + order.getOrderCode());
                            invoice.setOrder(order);
                            invoice.setOrderCode(order.getOrderCode());
                            invoice.setCustomer(order.getCustomer());
                            invoice.setCustomerName(order.getCustomerName());
                            invoice.setCustomerEmail(order.getCustomerEmail());
                            invoice.setCustomerAddress(order.getCustomer() != null ? order.getCustomer().getAddress() : null);
                            invoice.setBaseTotal(order.getBaseTotal() != null ? order.getBaseTotal() : 0.0);
                            invoice.setTotalDiscount(order.getTotalDiscount() != null ? order.getTotalDiscount() : 0.0);
                            invoice.setTotalTax(order.getTotalTax() != null ? order.getTotalTax() : 0.0);
                            invoice.setFinalAmount(order.getTotalPrice());
                            invoice.setStatus("PENDING");
                            invoice.setInvoiceDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());
                            invoice.setUpdatedAt(LocalDateTime.now());
                            invoiceRepository.save(invoice);
                        } catch (Exception ex) {
                            // Silently ignore individual invoice generation errors
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore sync failures
        }

        return invoiceRepository.searchAndFilter(search, status, customerId, PageRequest.of(page, size))
                .map(this::mapToDto);
    }

    @Override
    @CacheEvict(value = "invoices", allEntries = true)
    public void delete(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice Not Found"));
        invoice.setDeleted(true);
        invoiceRepository.save(invoice);
    }

    private InvoiceResponseDto mapToDto(Invoice invoice) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(invoice.getId());
        dto.setInvoiceCode(invoice.getInvoiceCode());
        dto.setOrderCode(invoice.getOrderCode());
        dto.setCustomerName(invoice.getCustomerName());
        dto.setCustomerEmail(invoice.getCustomerEmail());
        dto.setCustomerAddress(invoice.getCustomerAddress());
        dto.setBaseTotal(invoice.getBaseTotal());
        dto.setTotalDiscount(invoice.getTotalDiscount());
        dto.setTotalTax(invoice.getTotalTax());
        dto.setFinalAmount(invoice.getFinalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        return dto;
    }

    @Override
    public byte[] exportToExcel(java.util.List<Invoice> invoices) throws java.io.IOException {
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Invoices");
            sheet.setDefaultColumnWidth(20);

            String[] exportHeaders = { "Invoice Code", "Order Code", "Customer Name", "Customer Email", "Customer Address", "Base Total", "Total Discount", "Total Tax", "Final Amount", "Status", "Invoice Date" };

            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            com.hepl.product.Util.ExportUtils.setPoiCell(titleRow, 0, "Invoice History Export | " + java.time.LocalDate.now(),
                    com.hepl.product.Util.ExportUtils.createTitleStyle(wb));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, exportHeaders.length - 1));

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            org.apache.poi.ss.usermodel.CellStyle hStyle = com.hepl.product.Util.ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < exportHeaders.length; i++) {
                com.hepl.product.Util.ExportUtils.setPoiCell(headerRow, i, exportHeaders[i], hStyle);
            }

            org.apache.poi.ss.usermodel.CellStyle even = com.hepl.product.Util.ExportUtils.createEvenRowStyle(wb);
            org.apache.poi.ss.usermodel.CellStyle odd = com.hepl.product.Util.ExportUtils.createOddRowStyle(wb);
            int rowIndex = 2;
            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex++);
                org.apache.poi.ss.usermodel.CellStyle style = (i % 2 == 0) ? even : odd;
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 0, inv.getInvoiceCode(), style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 1, inv.getOrderCode(), style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 2, inv.getCustomerName() != null ? inv.getCustomerName() : "-", style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 3, inv.getCustomerEmail() != null ? inv.getCustomerEmail() : "-", style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 4, inv.getCustomerAddress() != null ? inv.getCustomerAddress() : "-", style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 5, String.valueOf(inv.getBaseTotal()), style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 6, String.valueOf(inv.getTotalDiscount()), style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 7, String.valueOf(inv.getTotalTax()), style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 8, String.valueOf(inv.getFinalAmount()), style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 9, inv.getStatus() != null ? inv.getStatus() : "PENDING", style);
                com.hepl.product.Util.ExportUtils.setPoiCell(row, 10, inv.getInvoiceDate() != null ? inv.getInvoiceDate().toLocalDate().toString() : "-", style);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(java.util.List<Invoice> invoices) throws java.io.IOException {
        return exportToPdf(invoices, "CLASSIC");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] exportToPdf(java.util.List<Invoice> invoices, String template) throws java.io.IOException {
        String tpl = (template != null) ? template.toUpperCase() : "CLASSIC";
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        
        byte[] logoBytes = null;
        try {
            org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("sense_logo.png");
            try (java.io.InputStream is = resource.getInputStream()) {
                logoBytes = is.readAllBytes();
            }
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
        }
        
        com.itextpdf.kernel.geom.PageSize pageSize = com.itextpdf.kernel.geom.PageSize.A4;
        if ("THERMAL".equals(tpl) && invoices.size() == 1) {
            pageSize = new com.itextpdf.kernel.geom.PageSize(226, 600);
        }

        try (com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(out);
             com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
             com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf, pageSize)) {
            
            if ("THERMAL".equals(tpl) && invoices.size() == 1) {
                doc.setMargins(10, 10, 10, 10);
            }

            com.itextpdf.kernel.font.PdfFont bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            com.itextpdf.kernel.font.PdfFont reg = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            
            com.itextpdf.kernel.colors.DeviceRgb hBg;
            com.itextpdf.kernel.colors.DeviceRgb evenBg = new com.itextpdf.kernel.colors.DeviceRgb(248, 249, 255);
            com.itextpdf.kernel.colors.DeviceRgb white = new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255);

            if ("MODERN".equals(tpl)) {
                hBg = new com.itextpdf.kernel.colors.DeviceRgb(15, 118, 110); // Teal-Green
            } else if ("MINIMALIST".equals(tpl)) {
                hBg = new com.itextpdf.kernel.colors.DeviceRgb(31, 41, 55); // Charcoal Dark
            } else {
                hBg = new com.itextpdf.kernel.colors.DeviceRgb(99, 102, 241); // Indigo
            }

            if (invoices.size() == 1) {
                Invoice inv = invoices.get(0);
                Order order = inv.getOrder();
                
                if ("THERMAL".equals(tpl)) {
                    // --- 1. THERMAL RECEIPT TEMPLATE ---
                    doc.add(new com.itextpdf.layout.element.Paragraph("📦 INVENTORY PRO").setFont(bold).setFontSize(10).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    doc.add(new com.itextpdf.layout.element.Paragraph("support@inventorypro.com\nTel: +91 98765 43210").setFont(reg).setFontSize(7).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    doc.add(new com.itextpdf.layout.element.Paragraph("--------------------------------------------------").setFont(reg).setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    
                    doc.add(new com.itextpdf.layout.element.Paragraph("INV: #" + inv.getInvoiceCode()).setFont(bold).setFontSize(7.5f));
                    doc.add(new com.itextpdf.layout.element.Paragraph("Date: " + (inv.getInvoiceDate() != null ? inv.getInvoiceDate().toLocalDate().toString() : "-")).setFont(reg).setFontSize(7));
                    doc.add(new com.itextpdf.layout.element.Paragraph("Cust: " + inv.getCustomerName()).setFont(reg).setFontSize(7));
                    doc.add(new com.itextpdf.layout.element.Paragraph("--------------------------------------------------").setFont(reg).setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    
                    float[] tWidths = { 95f, 30f, 35f, 45f };
                    com.itextpdf.layout.element.Table tTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(tWidths)).useAllAvailableWidth();
                    tTable.addHeaderCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Item").setFont(bold).setFontSize(7)));
                    tTable.addHeaderCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Qty").setFont(bold).setFontSize(7)));
                    tTable.addHeaderCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("GST").setFont(bold).setFontSize(7)));
                    tTable.addHeaderCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("Total").setFont(bold).setFontSize(7)));
                    
                    if (order != null && order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                        for (com.hepl.product.model.OrderItem item : order.getOrderItems()) {
                            tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph(item.getProductName()).setFont(reg).setFontSize(7)));
                            tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph(String.valueOf(item.getQuantity())).setFont(reg).setFontSize(7)));
                            tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph(item.getGstpercentage() + "%").setFont(reg).setFontSize(7)));
                            tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("₹" + item.getTotalPrice()).setFont(bold).setFontSize(7)));
                        }
                    } else {
                        tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("General Item").setFont(reg).setFontSize(7)));
                        tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("1").setFont(reg).setFontSize(7)));
                        tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("0%").setFont(reg).setFontSize(7)));
                        tTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getFinalAmount()).setFont(bold).setFontSize(7)));
                    }
                    doc.add(tTable);
                    doc.add(new com.itextpdf.layout.element.Paragraph("--------------------------------------------------").setFont(reg).setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    
                    doc.add(new com.itextpdf.layout.element.Paragraph("Subtotal: ₹" + inv.getBaseTotal()).setFont(reg).setFontSize(7).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    doc.add(new com.itextpdf.layout.element.Paragraph("Discount: -₹" + inv.getTotalDiscount()).setFont(reg).setFontSize(7).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    doc.add(new com.itextpdf.layout.element.Paragraph("Tax (GST): ₹" + inv.getTotalTax()).setFont(reg).setFontSize(7).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    doc.add(new com.itextpdf.layout.element.Paragraph("GRAND TOTAL: ₹" + inv.getFinalAmount()).setFont(bold).setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    doc.add(new com.itextpdf.layout.element.Paragraph("--------------------------------------------------").setFont(reg).setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    doc.add(new com.itextpdf.layout.element.Paragraph("Thank you for shopping!").setFont(reg).setFontSize(7).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    
                } else if ("MINIMALIST".equals(tpl)) {
                    // --- 2. MINIMALIST BLACK-AND-WHITE TEMPLATE ---
                    float[] headerWidths = { 280f, 280f };
                    com.itextpdf.layout.element.Table headerTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(headerWidths)).useAllAvailableWidth();
                    headerTable.setBorder(null);
                    
                    com.itextpdf.layout.element.Cell companyCell = new com.itextpdf.layout.element.Cell().setBorder(null);
                    if (logoBytes != null) {
                        try {
                            com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(logoBytes);
                            com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(imageData);
                            logo.scaleToFit(100, 50);
                            logo.setMarginBottom(5);
                            companyCell.add(logo);
                        } catch (Exception e) {
                            companyCell.add(new com.itextpdf.layout.element.Paragraph("INVENTORY PRO").setFont(bold).setFontSize(14).setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK));
                        }
                    } else {
                        companyCell.add(new com.itextpdf.layout.element.Paragraph("INVENTORY PRO").setFont(bold).setFontSize(14).setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK));
                    }
                    companyCell.add(new com.itextpdf.layout.element.Paragraph("Simple, Clean & Corporate").setFont(reg).setFontSize(8.5f).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
                    headerTable.addCell(companyCell);
                    
                    com.itextpdf.layout.element.Cell invoiceInfoCell = new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT);
                    invoiceInfoCell.add(new com.itextpdf.layout.element.Paragraph("INVOICE").setFont(bold).setFontSize(16).setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK));
                    invoiceInfoCell.add(new com.itextpdf.layout.element.Paragraph("INV: #" + inv.getInvoiceCode()).setFont(reg).setFontSize(9));
                    invoiceInfoCell.add(new com.itextpdf.layout.element.Paragraph("Date: " + (inv.getInvoiceDate() != null ? inv.getInvoiceDate().toLocalDate().toString() : "-")).setFont(reg).setFontSize(8.5f));
                    headerTable.addCell(invoiceInfoCell);
                    doc.add(headerTable);

                    doc.add(new com.itextpdf.layout.element.Paragraph("").setMarginBottom(10));
                    
                    com.itextpdf.layout.element.Table detailsTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(new float[]{280f, 280f})).useAllAvailableWidth();
                    detailsTable.setBorder(null);
                    
                    com.itextpdf.layout.element.Cell billToCell = new com.itextpdf.layout.element.Cell().setBorder(null);
                    billToCell.add(new com.itextpdf.layout.element.Paragraph("CLIENT").setFont(bold).setFontSize(9).setMarginBottom(2));
                    billToCell.add(new com.itextpdf.layout.element.Paragraph(inv.getCustomerName()).setFont(reg).setFontSize(9.5f));
                    billToCell.add(new com.itextpdf.layout.element.Paragraph(inv.getCustomerEmail()).setFont(reg).setFontSize(8.5f).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
                    billToCell.add(new com.itextpdf.layout.element.Paragraph(inv.getCustomerAddress()).setFont(reg).setFontSize(8.5f).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
                    detailsTable.addCell(billToCell);
                    
                    com.itextpdf.layout.element.Cell orderMetaCell = new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT);
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("ORDER").setFont(bold).setFontSize(9).setMarginBottom(2));
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("Code: " + inv.getOrderCode()).setFont(reg).setFontSize(8.5f));
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("Status: " + (order != null ? order.getStatus() : "PENDING")).setFont(reg).setFontSize(8.5f));
                    detailsTable.addCell(orderMetaCell);
                    doc.add(detailsTable);

                    doc.add(new com.itextpdf.layout.element.Paragraph("").setMarginBottom(12));

                    float[] widths = { 30f, 200f, 60f, 50f, 50f, 50f, 80f };
                    com.itextpdf.layout.element.Table itemTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(widths)).useAllAvailableWidth();
                    
                    String[] cols = { "#", "Description", "Unit Price", "Qty", "Disc", "GST", "Amount" };
                    for (String col : cols) {
                        itemTable.addHeaderCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.BLACK, 1)).add(new com.itextpdf.layout.element.Paragraph(col).setFont(bold).setFontSize(8.5f)).setPadding(4));
                    }
                    
                    int idx = 1;
                    if (order != null && order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                        for (com.hepl.product.model.OrderItem item : order.getOrderItems()) {
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph(String.valueOf(idx++)).setFont(reg).setFontSize(8.5f)).setPadding(4));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph(item.getProductName()).setFont(reg).setFontSize(8.5f)).setPadding(4));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("₹" + item.getPrice()).setFont(reg).setFontSize(8.5f)).setPadding(4));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph(String.valueOf(item.getQuantity())).setFont(reg).setFontSize(8.5f)).setPadding(4));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph(item.getDiscount() + "%").setFont(reg).setFontSize(8.5f)).setPadding(4));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph(item.getGstpercentage() + "%").setFont(reg).setFontSize(8.5f)).setPadding(4));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("₹" + item.getTotalPrice()).setFont(bold).setFontSize(8.5f)).setPadding(4));
                        }
                    } else {
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("1").setFont(reg).setFontSize(8.5f)).setPadding(4));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("General Purchase").setFont(reg).setFontSize(8.5f)).setPadding(4));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getBaseTotal()).setFont(reg).setFontSize(8.5f)).setPadding(4));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("1").setFont(reg).setFontSize(8.5f)).setPadding(4));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("0%").setFont(reg).setFontSize(8.5f)).setPadding(4));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("0%").setFont(reg).setFontSize(8.5f)).setPadding(4));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, 0.5f)).add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getFinalAmount()).setFont(bold).setFontSize(8.5f)).setPadding(4));
                    }
                    doc.add(itemTable);

                    doc.add(new com.itextpdf.layout.element.Paragraph("").setMarginBottom(10));
                    
                    com.itextpdf.layout.element.Table totalTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(new float[]{340f, 180f})).useAllAvailableWidth();
                    totalTable.setBorder(null);
                    totalTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Thank you for your business.").setFont(reg).setFontSize(8).setItalic()));
                    
                    com.itextpdf.layout.element.Cell rightCell = new com.itextpdf.layout.element.Cell().setBorder(null);
                    rightCell.add(new com.itextpdf.layout.element.Paragraph("Subtotal: ₹" + inv.getBaseTotal()).setFont(reg).setFontSize(8.5f).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    rightCell.add(new com.itextpdf.layout.element.Paragraph("Discount: -₹" + inv.getTotalDiscount()).setFont(reg).setFontSize(8.5f).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    rightCell.add(new com.itextpdf.layout.element.Paragraph("Tax (GST): ₹" + inv.getTotalTax()).setFont(reg).setFontSize(8.5f).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    rightCell.add(new com.itextpdf.layout.element.Paragraph("Total: ₹" + inv.getFinalAmount()).setFont(bold).setFontSize(10).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
                    totalTable.addCell(rightCell);
                    doc.add(totalTable);

                } else {
                    // --- 3. CLASSIC INDIGO & MODERN TEAL TEMPLATES ---
                    float[] headerWidths = { 280f, 280f };
                    com.itextpdf.layout.element.Table headerTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(headerWidths)).useAllAvailableWidth();
                    headerTable.setBorder(null);
                    
                    com.itextpdf.layout.element.Cell companyCell = new com.itextpdf.layout.element.Cell().setBorder(null);
                    if (logoBytes != null) {
                        try {
                            com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(logoBytes);
                            com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(imageData);
                            logo.scaleToFit(100, 50);
                            logo.setMarginBottom(5);
                            companyCell.add(logo);
                        } catch (Exception e) {
                            companyCell.add(new com.itextpdf.layout.element.Paragraph("Inventory Pro").setFont(bold).setFontSize(16).setFontColor(hBg));
                        }
                    } else {
                        companyCell.add(new com.itextpdf.layout.element.Paragraph("Inventory Pro").setFont(bold).setFontSize(16).setFontColor(hBg));
                    }
                    companyCell.add(new com.itextpdf.layout.element.Paragraph("Product Inventory Management").setFont(reg).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
                    companyCell.add(new com.itextpdf.layout.element.Paragraph("support@inventorypro.com").setFont(reg).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
                    headerTable.addCell(companyCell);
                    
                    com.itextpdf.layout.element.Cell invoiceInfoCell = new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT);
                    invoiceInfoCell.add(new com.itextpdf.layout.element.Paragraph("INVOICE").setFont(bold).setFontSize(18).setFontColor(hBg));
                    invoiceInfoCell.add(new com.itextpdf.layout.element.Paragraph("#" + inv.getInvoiceCode()).setFont(bold).setFontSize(10).setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK));
                    invoiceInfoCell.add(new com.itextpdf.layout.element.Paragraph("Date: " + (inv.getInvoiceDate() != null ? inv.getInvoiceDate().toLocalDate().toString() : "-")).setFont(reg).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
                    headerTable.addCell(invoiceInfoCell);
                    doc.add(headerTable);

                    doc.add(new com.itextpdf.layout.element.Paragraph("______________________________________________________________________________")
                            .setFont(reg).setFontColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setMarginBottom(16));

                    com.itextpdf.layout.element.Table detailsTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(new float[]{280f, 280f})).useAllAvailableWidth();
                    detailsTable.setBorder(null);
                    
                    com.itextpdf.layout.element.Cell billToCell = new com.itextpdf.layout.element.Cell().setBorder(null);
                    billToCell.add(new com.itextpdf.layout.element.Paragraph("BILL TO").setFont(bold).setFontSize(10).setFontColor(hBg).setMarginBottom(4));
                    billToCell.add(new com.itextpdf.layout.element.Paragraph(inv.getCustomerName() != null ? inv.getCustomerName() : "-").setFont(bold).setFontSize(11));
                    billToCell.add(new com.itextpdf.layout.element.Paragraph(inv.getCustomerEmail() != null ? inv.getCustomerEmail() : "-").setFont(reg).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
                    billToCell.add(new com.itextpdf.layout.element.Paragraph(inv.getCustomerAddress() != null ? inv.getCustomerAddress() : "-").setFont(reg).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
                    detailsTable.addCell(billToCell);
                    
                    com.itextpdf.layout.element.Cell orderMetaCell = new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT);
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("ORDER INFO").setFont(bold).setFontSize(10).setFontColor(hBg).setMarginBottom(4));
                    String paymentStatus = order != null ? order.getPaymentstatus() : "PENDING";
                    String orderStatus = order != null ? order.getStatus() : "PENDING";
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("Order Code: " + inv.getOrderCode()).setFont(reg).setFontSize(9));
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("Order Status: " + orderStatus).setFont(reg).setFontSize(9));
                    orderMetaCell.add(new com.itextpdf.layout.element.Paragraph("Payment: " + paymentStatus).setFont(bold).setFontSize(9).setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(22, 101, 52)));
                    detailsTable.addCell(orderMetaCell);
                    doc.add(detailsTable);

                    doc.add(new com.itextpdf.layout.element.Paragraph("").setMarginBottom(16));

                    float[] widths = { 30f, 180f, 70f, 50f, 50f, 50f, 80f };
                    com.itextpdf.layout.element.Table itemTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(widths)).useAllAvailableWidth();
                    
                    String[] cols = { "#", "Product", "Price", "Qty", "Disc", "GST", "Total" };
                    for (String col : cols) {
                        itemTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(col).setFont(bold).setFontSize(9)).setBackgroundColor(hBg).setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE).setPadding(6).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                    }
                    
                    if (order != null && order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                        int index = 1;
                        for (com.hepl.product.model.OrderItem item : order.getOrderItems()) {
                            com.itextpdf.kernel.colors.DeviceRgb rowBg = (index % 2 == 0) ? evenBg : white;
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(index++)).setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(item.getProductName()).setFont(bold).setFontSize(9)).setBackgroundColor(rowBg).setPadding(5));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("₹" + item.getPrice()).setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(item.getQuantity())).setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(item.getDiscount() + "%").setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(item.getGstpercentage() + "%").setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                            itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("₹" + item.getTotalPrice()).setFont(bold).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                        }
                    } else {
                        com.itextpdf.kernel.colors.DeviceRgb rowBg = white;
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("1").setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("General Purchase Item").setFont(bold).setFontSize(9)).setBackgroundColor(rowBg).setPadding(5));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getBaseTotal()).setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("1").setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("0%").setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("0%").setFont(reg).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                        itemTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getFinalAmount()).setFont(bold).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(5));
                    }
                    doc.add(itemTable);

                    com.itextpdf.layout.element.Table summaryTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(new float[]{300f, 210f})).useAllAvailableWidth();
                    summaryTable.setMarginTop(12);
                    summaryTable.setBorder(null);
                    
                    com.itextpdf.layout.element.Cell thankYouCell = new com.itextpdf.layout.element.Cell().setBorder(null);
                    thankYouCell.add(new com.itextpdf.layout.element.Paragraph("Thank you for your business!").setFont(reg).setFontSize(10).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY).setItalic());
                    thankYouCell.add(new com.itextpdf.layout.element.Paragraph("Computer generated invoice. No signature required.").setFont(reg).setFontSize(8).setFontColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                    summaryTable.addCell(thankYouCell);
                    
                    com.itextpdf.layout.element.Cell totalsCell = new com.itextpdf.layout.element.Cell();
                    totalsCell.setPadding(8);
                    
                    com.itextpdf.layout.element.Table miniTable = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{50f, 50f})).useAllAvailableWidth();
                    miniTable.setBorder(null);
                    
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Subtotal:").setFont(reg).setFontSize(9)));
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getBaseTotal()).setFont(reg).setFontSize(9)));
                    
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Discount:").setFont(reg).setFontSize(9)));
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("-₹" + inv.getTotalDiscount()).setFont(reg).setFontSize(9)));
                    
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Tax (GST):").setFont(reg).setFontSize(9)));
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getTotalTax()).setFont(reg).setFontSize(9)));
                    
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).add(new com.itextpdf.layout.element.Paragraph("Grand Total:").setFont(bold).setFontSize(10).setFontColor(hBg)));
                    miniTable.addCell(new com.itextpdf.layout.element.Cell().setBorder(null).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT).add(new com.itextpdf.layout.element.Paragraph("₹" + inv.getFinalAmount()).setFont(bold).setFontSize(10).setFontColor(hBg)));
                    
                    totalsCell.add(miniTable);
                    summaryTable.addCell(totalsCell);
                    doc.add(summaryTable);
                }
                
            } else {
                // Bulk Invoice List
                com.hepl.product.Util.ExportUtils.addPdfTitle(doc, "Invoice History Report", invoices.size(), bold, reg);

                float[] widths = { 40f, 90f, 90f, 120f, 80f, 60f, 80f };
                com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPointArray(widths)).useAllAvailableWidth();
                String[] cols = { "#", "Invoice Code", "Order Code", "Customer", "Amount", "Status", "Date" };
                for (String col : cols)
                    table.addHeaderCell(com.hepl.product.Util.ExportUtils.pdfHeaderCell(col, bold, hBg));

                for (int i = 0; i < invoices.size(); i++) {
                    Invoice inv = invoices.get(i);
                    com.itextpdf.kernel.colors.DeviceRgb bg = (i % 2 == 0) ? evenBg : white;
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell(String.valueOf(i + 1), reg, bg, true));
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell(inv.getInvoiceCode(), reg, bg, false));
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell(inv.getOrderCode(), reg, bg, false));
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell(inv.getCustomerName() != null ? inv.getCustomerName() : "-", reg, bg, false));
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell("₹" + inv.getFinalAmount(), reg, bg, true));
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell(inv.getStatus(), reg, bg, true));
                    table.addCell(com.hepl.product.Util.ExportUtils.pdfDataCell(
                            inv.getInvoiceDate() != null ? inv.getInvoiceDate().toLocalDate().toString() : "-", reg, bg, true));
                }
                doc.add(table);
            }
        }
        return out.toByteArray();
    }
}
