package com.hepl.product.Service.ServiceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;
import com.hepl.product.Repository.DivisionRepository;
import com.hepl.product.Service.ProductImportExportService;
import com.hepl.product.Service.ProductService;
import com.hepl.product.model.Division;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import lombok.RequiredArgsConstructor;

/**
 * Implements bulk import (Excel) and export (Excel + PDF) for Products.
 *
 * IMPORTANT: POI's Cell / HorizontalAlignment and iText's Cell /
 * HorizontalAlignment
 * clash. We always reference them with fully-qualified names to avoid
 * ambiguity.
 */
@Service
@RequiredArgsConstructor
public class ProductImportExportServiceImpl implements ProductImportExportService {

    private final ProductService productService;
    private final DivisionRepository divisionRepository;
    private final com.hepl.product.Repository.ProductRepository productRepository;

    // ─── Column definitions ────────────────────────────────────────────────────
    private static final String[] HEADERS = {
            "Name*", "Price*", "Quantity*", "Division Name*",
            "SKU*", "UOM*", "Expiry Date* (YYYY-MM-DD)", "Batch Code"
    };

    private static final String[] SAMPLE = {
            "Product Alpha", "299.99", "100", "Electronics",
            "SKU-001", "PCS", "2026-12-31", "BAT-001"
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // IMPORT
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();

            // Skip header row (row 0 = instructions, row 1 = headers in template)
            if (rowIter.hasNext())
                rowIter.next(); // skip row 0
            if (rowIter.hasNext())
                rowIter.next(); // skip row 1 (headers)

            int rowNum = 3;
            java.util.Set<String> seenNamesInSheet = new java.util.HashSet<>();

            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", rowNum++);

                try {
                    String name = getCellStr(row, 0).trim();
                    String priceStr = getCellStr(row, 1).trim();
                    String qtyStr = getCellStr(row, 2).trim();
                    String divName = getCellStr(row, 3).trim();
                    String sku = getCellStr(row, 4).trim();
                    String uom = getCellStr(row, 5).trim();
                    String expiryStr = getCellStr(row, 6).trim();
                    String batchcode = getCellStr(row, 7).trim();

                    // Skip fully empty rows
                    if (name.isBlank() && priceStr.isBlank() && divName.isBlank())
                        continue;

                    // Validate required fields
                    List<String> errors = new ArrayList<>();
                    if (name.isBlank())
                        errors.add("Name is required");
                    if (priceStr.isBlank())
                        errors.add("Price is required");
                    if (qtyStr.isBlank())
                        errors.add("Quantity is required");
                    if (divName.isBlank())
                        errors.add("Division Name is required");
                    if (sku.isBlank())
                        errors.add("SKU is required");
                    if (uom.isBlank())
                        errors.add("UOM is required");
                    if (expiryStr.isBlank())
                        errors.add("Expiry Date is required");

                    LocalDate expiryDate = null;
                    if (!expiryStr.isBlank()) {
                        expiryDate = parseExpiryDate(expiryStr);
                        if (expiryDate == null) {
                            errors.add("Invalid Expiry Date format. Expected YYYY-MM-DD or DD-MM-YYYY");
                        }
                    }

                    if (errors.isEmpty()) {
                        String normName = name.toLowerCase().trim();
                        if (seenNamesInSheet.contains(normName)) {
                            errors.add("Duplicate product name found in this Excel sheet");
                        } else {
                            seenNamesInSheet.add(normName);
                        }
                    }

                    if (errors.isEmpty()) {
                        List<com.hepl.product.model.Product> productsList = productRepository
                                .findAllByNameIgnoreCase(name);
                        boolean alreadyExists = productsList.stream().anyMatch(p -> !p.isDeleted());
                        if (alreadyExists) {
                            errors.add("A product with this name already exists");
                        }
                    }

                    if (!errors.isEmpty()) {
                        rowResult.put("status", "FAILED");
                        rowResult.put("name", name.isBlank() ? "-" : name);
                        rowResult.put("errors", errors);
                        results.add(rowResult);
                        failed++;
                        continue;
                    }

                    double price = Double.parseDouble(priceStr);
                    int qty = (int) Double.parseDouble(qtyStr);

                    List<Division> divisions = divisionRepository.findAllByNameIgnoreCase(divName);
                    Division division = divisions.stream()
                            .filter(d -> !d.isDeleted())
                            .findFirst()
                            .orElse(null);

                    if (division == null) {
                        division = new Division();
                        division.setName(divName);
                        division.setDeleted(false);
                        division = divisionRepository.save(division);
                    }

                    if (!errors.isEmpty()) {
                        rowResult.put("status", "FAILED");
                        rowResult.put("name", name);
                        rowResult.put("errors", errors);
                        results.add(rowResult);
                        failed++;
                        continue;
                    }

                    // Build and persist
                    ProductRequestDto dto = new ProductRequestDto();
                    dto.setName(name);
                    dto.setPrice(price);
                    dto.setQuantity(qty);
                    dto.setDivisionName(divName);
                    dto.setSku(sku);
                    dto.setUom(uom);
                    dto.setExpiryDate(expiryDate);
                    String cleanBcode = batchcode != null ? batchcode.trim() : "";
                    if (cleanBcode.equalsIgnoreCase("NA") || cleanBcode.equalsIgnoreCase("N/A") || cleanBcode.equalsIgnoreCase("NULL")) {
                        cleanBcode = "";
                    }
                    dto.setBatchcode(cleanBcode);

                    ProductResponseDto saved = productService.save(dto);
                    rowResult.put("status", "SUCCESS");
                    rowResult.put("name", saved.getName());
                    rowResult.put("id", saved.getId());
                    results.add(rowResult);
                    success++;

                } catch (Exception ex) {
                    rowResult.put("status", "FAILED");
                    rowResult.put("errors", List.of("Unexpected error: " + ex.getMessage()));
                    results.add(rowResult);
                    failed++;
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalProcessed", success + failed);
        response.put("success", success);
        response.put("failed", failed);
        response.put("rows", results);

        response.put("totalRows", success + failed);
        response.put("successCount", success);
        response.put("failedCount", failed);
        List<Map<String, Object>> failedRowsList = new ArrayList<>();
        for (Map<String, Object> r : results) {
            if ("FAILED".equals(r.get("status"))) {
                failedRowsList.add(r);
            }
        }
        response.put("failedRows", failedRowsList);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT — EXCEL
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public byte[] exportToExcel(List<ProductResponseDto> products) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Products");
            sheet.setDefaultColumnWidth(18);

            String[] exportHeaders = HEADERS;

            // ── Title row ──────────────────────────────────────────────────────
            CellStyle titleStyle = createTitleStyle(wb);
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Product Inventory Export  |  " + LocalDate.now());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, exportHeaders.length - 1));

            // ── Header row ─────────────────────────────────────────────────────
            CellStyle headerStyle = createHeaderStyle(wb);
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            for (int c = 0; c < exportHeaders.length; c++) {
                org.apache.poi.ss.usermodel.Cell hCell = headerRow.createCell(c);
                hCell.setCellValue(exportHeaders[c]);
                hCell.setCellStyle(headerStyle);
            }

            // ── Data rows ──────────────────────────────────────────────────────
            CellStyle evenStyle = createEvenRowStyle(wb);
            CellStyle oddStyle = createOddRowStyle(wb);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (int i = 0; i < products.size(); i++) {
                ProductResponseDto p = products.get(i);
                Row row = sheet.createRow(i + 2);
                row.setHeightInPoints(20);
                CellStyle rowStyle = (i % 2 == 0) ? evenStyle : oddStyle;

                setPoiCell(row, 0, p.getName() != null ? p.getName() : "", rowStyle);
                setPoiCell(row, 1, String.format("%.2f", p.getPrice()), rowStyle);
                setPoiCell(row, 2, String.valueOf(p.getQuantity()), rowStyle);
                setPoiCell(row, 3, p.getDivisionName() != null ? p.getDivisionName() : "", rowStyle);
                setPoiCell(row, 4, p.getSku() != null ? p.getSku() : "", rowStyle);
                setPoiCell(row, 5, p.getUom() != null ? p.getUom() : "", rowStyle);
                setPoiCell(row, 6, p.getExpiryDate() != null ? p.getExpiryDate().format(dtf) : "", rowStyle);
                setPoiCell(row, 7, p.getBatchcode() != null ? p.getBatchcode() : "", rowStyle);
            }

            // Auto-size
            for (int c = 0; c < exportHeaders.length; c++)
                sheet.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT — PDF
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public byte[] exportToPdf(List<ProductResponseDto> products) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf, PageSize.A4.rotate())) {

            doc.setMargins(30, 30, 30, 30);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            DeviceRgb headerBg = new DeviceRgb(99, 102, 241);
            DeviceRgb evenBg = new DeviceRgb(248, 249, 255);
            DeviceRgb titleColor = new DeviceRgb(60, 60, 120);

            // ── Title ──────────────────────────────────────────────────────────
            doc.add(new Paragraph("Product Inventory Report")
                    .setFont(boldFont).setFontSize(20)
                    .setFontColor(titleColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4));

            doc.add(new Paragraph("Generated: " + LocalDate.now()
                    + "   |   Total Products: " + products.size())
                    .setFont(regularFont).setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(16));

            // ── Table ──────────────────────────────────────────────────────────
            float[] colWidths = { 30f, 140f, 65f, 50f, 95f, 70f, 50f, 90f, 90f };
            Table table = new Table(UnitValue.createPointArray(colWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            // Use fully qualified iText HorizontalAlignment to avoid clash with POI's
            table.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

            String[] cols = { "#", "Name", "Price (Rs)", "Qty", "Division",
                    "SKU", "UOM", "Expiry Date", "Batch Code" };
            for (String col : cols) {
                table.addHeaderCell(
                        new com.itextpdf.layout.element.Cell()
                                .add(new Paragraph(col != null ? col : "").setFont(boldFont).setFontSize(8))
                                .setBackgroundColor(headerBg)
                                .setFontColor(ColorConstants.WHITE)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(6));
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (int i = 0; i < products.size(); i++) {
                ProductResponseDto p = products.get(i);
                DeviceRgb rowBg = (i % 2 == 0) ? evenBg : new DeviceRgb(255, 255, 255);
                String[] values = {
                        String.valueOf(i + 1),
                        p.getName() != null ? p.getName() : "",
                        String.format("%.2f", p.getPrice()),
                        String.valueOf(p.getQuantity()),
                        p.getDivisionName() != null ? p.getDivisionName() : "",
                        p.getSku() != null ? p.getSku() : "",
                        p.getUom() != null ? p.getUom() : "",
                        p.getExpiryDate() != null ? p.getExpiryDate().format(dtf) : "",
                        p.getBatchcode() != null ? p.getBatchcode() : ""
                };
                for (int c = 0; c < values.length; c++) {
                    boolean isNum = (c == 0 || c == 2 || c == 3);
                    table.addCell(
                            new com.itextpdf.layout.element.Cell()
                                    .add(new Paragraph(values[c] != null ? values[c] : "").setFont(regularFont).setFontSize(8))
                                    .setBackgroundColor(rowBg)
                                    .setTextAlignment(isNum ? TextAlignment.CENTER : TextAlignment.LEFT)
                                    .setPadding(5));
                }
            }

            doc.add(table);

            // ── Footer ─────────────────────────────────────────────────────────
            doc.add(new Paragraph("\nProduct Inventory Management System  —  Confidential")
                    .setFont(regularFont).setFontSize(7)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(12));
        }

        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Products Template");
            sheet.setDefaultColumnWidth(24);

            // ── Instructions row ───────────────────────────────────────────────
            CellStyle infoStyle = createInfoStyle(wb);
            Row infoRow = sheet.createRow(0);
            infoRow.setHeightInPoints(20);
            org.apache.poi.ss.usermodel.Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(
                    "Fill in product data below. Columns marked * are required. "
                            + "Do NOT change column headers. Date format: YYYY-MM-DD");
            infoCell.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // ── Header row ─────────────────────────────────────────────────────
            CellStyle headerStyle = createHeaderStyle(wb);
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(26);
            for (int c = 0; c < HEADERS.length; c++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            // ── Sample row ─────────────────────────────────────────────────────
            CellStyle sampleStyle = createSampleStyle(wb);
            Row sampleRow = sheet.createRow(2);
            sampleRow.setHeightInPoints(20);
            for (int c = 0; c < SAMPLE.length; c++) {
                org.apache.poi.ss.usermodel.Cell cell = sampleRow.createCell(c);
                cell.setCellValue(SAMPLE[c]);
                cell.setCellStyle(sampleStyle);
            }

            // Auto-size
            for (int c = 0; c < HEADERS.length; c++)
                sheet.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POI Style helpers (all use org.apache.poi.ss.usermodel.* explicitly)
    // ═══════════════════════════════════════════════════════════════════════════
    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle createEvenRowStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        s.setBorderRight(BorderStyle.HAIR);
        return s;
    }

    private CellStyle createOddRowStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        s.setBorderRight(BorderStyle.HAIR);
        return s;
    }

    private CellStyle createInfoStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createSampleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        s.setBorderRight(BorderStyle.HAIR);
        return s;
    }

    // ─── Utility ──────────────────────────────────────────────────────────────
    private void setPoiCell(Row row, int col, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String getCellStr(Row row, int col) {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(col);
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d))
                    yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue().trim();
                }
            }
            default -> "";
        };
    }

    private LocalDate parseExpiryDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;

        // Try parsing as double/serial number (Excel serial format) first
        try {
            double numeric = Double.parseDouble(dateStr);
            java.util.Date javaDate = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(numeric);
            if (javaDate != null) {
                return javaDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception ignored) {
        }

        // Try standard patterns
        String[] patterns = {
                "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "d/M/yyyy", "d-M-yyyy",
                "yyyy/MM/dd", "dd.MM.yyyy", "yyyy.MM.dd", "yyyyMMdd", "d-MM-yyyy", "d/MM/yyyy"
        };
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
