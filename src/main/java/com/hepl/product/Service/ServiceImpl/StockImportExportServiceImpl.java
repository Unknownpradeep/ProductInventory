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
import com.hepl.product.model.Stock;
import com.hepl.product.model.Product;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Repository.StockRepository;
import com.hepl.product.Service.StockImportExportService;
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
public class StockImportExportServiceImpl implements StockImportExportService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    private static final String[] HEADERS = { "Product Name*", "Quantity*", "Type (IN/OUT)*" };
    private static final String[] SAMPLE = { "Laptop Pro", "50", "IN" };

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();
            if (rowIter.hasNext()) rowIter.next(); // skip info
            if (rowIter.hasNext()) rowIter.next(); // skip headers

            int rowNum = 3;
            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", rowNum++);

                try {
                    String productName = ExportUtils.getCellStr(row, 0);
                    String qtyStr = ExportUtils.getCellStr(row, 1);
                    String type = ExportUtils.getCellStr(row, 2).toUpperCase();

                    if (productName.isBlank()) continue;

                    List<String> errors = new ArrayList<>();
                    List<Product> prods = productRepository.findAllByNameIgnoreCase(productName);
                    Product foundProd = prods.stream()
                            .filter(p -> !p.isDeleted())
                            .findFirst()
                            .orElse(prods.isEmpty() ? null : prods.get(0));
                    if (foundProd == null) errors.add("Product not found: " + productName);
                    
                    double qty = 0;
                    try { qty = Double.parseDouble(qtyStr); }
                    catch (Exception e) { errors.add("Invalid quantity: " + qtyStr); }

                    if (!type.equals("IN") && !type.equals("OUT")) errors.add("Type must be IN or OUT");

                    if (!errors.isEmpty()) {
                        rowResult.put("status", "FAILED");
                        rowResult.put("errors", errors);
                        results.add(rowResult);
                        failed++;
                        continue;
                    }

                    Stock stock = new Stock();
                    stock.setProduct(foundProd);
                    stock.setProductName(foundProd.getName());
                    stock.setQuantity((int) qty);
                    stock.setType(type);
                    stock.setCreatedAt(LocalDate.now());

                    stockRepository.save(stock);
                    rowResult.put("status", "SUCCESS");
                    rowResult.put("name", productName);
                    results.add(rowResult);
                    success++;
                } catch (Exception e) {
                    rowResult.put("status", "FAILED");
                    rowResult.put("errors", List.of(e.getMessage()));
                    results.add(rowResult);
                    failed++;
                }
            }
        }
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
        return resp;
    }

    @Override
    public byte[] exportToExcel(List<Stock> stocks) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Stocks");
            sheet.setDefaultColumnWidth(20);

            String[] exportHeaders = HEADERS;
            
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            ExportUtils.setPoiCell(titleRow, 0, "Stock Transaction Export | " + LocalDate.now(), ExportUtils.createTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, exportHeaders.length - 1));

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < exportHeaders.length; i++) {
                ExportUtils.setPoiCell(headerRow, i, exportHeaders[i], hStyle);
            }

            CellStyle even = ExportUtils.createEvenRowStyle(wb);
            CellStyle odd = ExportUtils.createOddRowStyle(wb);
            for (int i = 0; i < stocks.size(); i++) {
                Stock s = stocks.get(i);
                Row row = sheet.createRow(i + 2);
                CellStyle style = (i % 2 == 0) ? even : odd;
                ExportUtils.setPoiCell(row, 0, s.getProductName() != null ? s.getProductName() : (s.getProduct() != null ? s.getProduct().getName() : "-"), style);
                ExportUtils.setPoiCell(row, 1, String.valueOf(s.getQuantity()), style);
                ExportUtils.setPoiCell(row, 2, s.getType(), style);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<Stock> stocks) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out); PdfDocument pdf = new PdfDocument(writer); Document doc = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            ExportUtils.addPdfTitle(doc, "Stock Transaction Report", stocks.size(), bold, reg);

            float[] widths = { 40f, 200f, 100f, 80f, 100f };
            Table table = new Table(UnitValue.createPointArray(widths)).useAllAvailableWidth();
            DeviceRgb hBg = new DeviceRgb(99, 102, 241);
            String[] cols = { "#", "Product", "Quantity", "Type", "Date" };
            for (String col : cols) table.addHeaderCell(ExportUtils.pdfHeaderCell(col, bold, hBg));

            DeviceRgb evenBg = new DeviceRgb(248, 249, 255);
            DeviceRgb white = new DeviceRgb(255, 255, 255);
            for (int i = 0; i < stocks.size(); i++) {
                Stock s = stocks.get(i);
                DeviceRgb bg = (i % 2 == 0) ? evenBg : white;
                table.addCell(ExportUtils.pdfDataCell(String.valueOf(i + 1), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(s.getProductName() != null ? s.getProductName() : (s.getProduct() != null ? s.getProduct().getName() : "-"), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(String.valueOf(s.getQuantity()), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(s.getType(), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "-", reg, bg, true));
            }
            doc.add(table);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Stock Template");
            sheet.setDefaultColumnWidth(25);
            Row info = sheet.createRow(0);
            ExportUtils.setPoiCell(info, 0, "Fill stock data. Type must be IN or OUT. Product Name must match existing product.", ExportUtils.createInfoStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
            Row hRow = sheet.createRow(1);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < HEADERS.length; i++) ExportUtils.setPoiCell(hRow, i, HEADERS[i], hStyle);
            Row sRow = sheet.createRow(2);
            CellStyle sStyle = ExportUtils.createSampleStyle(wb);
            for (int i = 0; i < SAMPLE.length; i++) ExportUtils.setPoiCell(sRow, i, SAMPLE[i], sStyle);
            wb.write(out);
            return out.toByteArray();
        }
    }
}
