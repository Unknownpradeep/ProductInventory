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
import com.hepl.product.Payload.Dto.CustomerDto.CustomerRequestDto;
import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;
import com.hepl.product.Service.CustomerImportExportService;
import com.hepl.product.Service.CustomerService;
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
public class CustomerImportExportServiceImpl implements CustomerImportExportService {

    private final CustomerService customerService;
    private final com.hepl.product.Repository.CustomerRepository customerRepository;

    private static final String[] HEADERS = { "Name*", "Email*", "Address", "State", "Country", "Pincode" };
    private static final String[] SAMPLE  = { "John Doe", "john@example.com", "123 Main St", "California", "USA", "90001" };

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();

            // Skip title/info row
            if (rowIter.hasNext()) rowIter.next();
            // Skip headers row
            if (rowIter.hasNext()) rowIter.next();

            int rowNum = 3;
            Set<String> seenEmailsInSheet = new HashSet<>();

            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", rowNum++);

                try {
                    String name    = ExportUtils.getCellStr(row, 0).trim();
                    String email   = ExportUtils.getCellStr(row, 1).trim();
                    String address = ExportUtils.getCellStr(row, 2).trim();
                    String state   = ExportUtils.getCellStr(row, 3).trim();
                    String country = ExportUtils.getCellStr(row, 4).trim();
                    String pincode = ExportUtils.getCellStr(row, 5).trim();

                    // Skip fully blank rows
                    if (name.isBlank() && email.isBlank() && address.isBlank()
                            && state.isBlank() && country.isBlank() && pincode.isBlank())
                        continue;

                    List<String> errors = new ArrayList<>();

                    // ── Required fields ──────────────────────────────────────
                    if (name.isBlank())
                        errors.add("Name is required");

                    if (email.isBlank()) {
                        errors.add("Email is required");
                    } else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                        // Permissive regex – same as the frontend validateRow
                        errors.add("Enter a valid email address");
                    }

                    // Address / State / Country are OPTIONAL – no required check
                    // Pincode: validate format only when a value is provided
                    if (!pincode.isBlank() && !pincode.matches("\\d{6}"))
                        errors.add("Pincode must be exactly 6 digits");

                    // ── In-sheet duplicate e-mail ────────────────────────────
                    if (errors.isEmpty()) {
                        String normEmail = email.toLowerCase();
                        if (seenEmailsInSheet.contains(normEmail)) {
                            errors.add("Duplicate email found in this Excel sheet");
                        } else {
                            seenEmailsInSheet.add(normEmail);
                        }
                    }

                    // ── DB duplicate check (List to handle duplicate DB rows) ─
                    if (errors.isEmpty()) {
                        List<com.hepl.product.model.Customer> existing =
                                customerRepository.findListByEmailIgnoreCase(email);
                        boolean activeExists = existing.stream().anyMatch(c -> !c.isDeleted());
                        if (activeExists)
                            errors.add("A customer with this email already exists");
                    }

                    // ── Persist or mark failed ───────────────────────────────
                    if (!errors.isEmpty()) {
                        rowResult.put("status", "FAILED");
                        rowResult.put("errors", errors);
                        results.add(rowResult);
                        failed++;
                        continue;
                    }

                    CustomerRequestDto dto = new CustomerRequestDto();
                    dto.setName(name);
                    dto.setEmail(email);
                    dto.setAddress(address);
                    dto.setState(state);
                    dto.setCountry(country);
                    dto.setPincode(pincode);

                    CustomerResponseDto saved = customerService.save(dto);
                    rowResult.put("status", "SUCCESS");
                    rowResult.put("name", saved.getName());
                    rowResult.put("id", saved.getId());
                    results.add(rowResult);
                    success++;

                } catch (Exception e) {
                    rowResult.put("status", "FAILED");
                    rowResult.put("errors",
                            List.of(e.getMessage() != null ? e.getMessage() : "Unexpected error"));
                    results.add(rowResult);
                    failed++;
                }
            }
        }

        // ── Build response ────────────────────────────────────────────────────
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalProcessed", success + failed);
        resp.put("success",        success);
        resp.put("failed",         failed);
        resp.put("rows",           results);
        resp.put("totalRows",      success + failed);
        resp.put("successCount",   success);
        resp.put("failedCount",    failed);

        List<Map<String, Object>> failedRows = new ArrayList<>();
        for (Map<String, Object> r : results)
            if ("FAILED".equals(r.get("status"))) failedRows.add(r);
        resp.put("failedRows", failedRows);
        return resp;
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportToExcel(List<CustomerResponseDto> customers) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Customers");
            sheet.setDefaultColumnWidth(20);

            String[] exportHeaders = HEADERS;

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            ExportUtils.setPoiCell(titleRow, 0,
                    "Customer Directory Export | " + LocalDate.now(),
                    ExportUtils.createTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, exportHeaders.length - 1));

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < exportHeaders.length; i++)
                ExportUtils.setPoiCell(headerRow, i, exportHeaders[i], hStyle);

            CellStyle even = ExportUtils.createEvenRowStyle(wb);
            CellStyle odd  = ExportUtils.createOddRowStyle(wb);
            for (int i = 0; i < customers.size(); i++) {
                CustomerResponseDto c = customers.get(i);
                Row row = sheet.createRow(i + 2);
                CellStyle s = (i % 2 == 0) ? even : odd;
                ExportUtils.setPoiCell(row, 0, c.getName(),    s);
                ExportUtils.setPoiCell(row, 1, c.getEmail(),   s);
                ExportUtils.setPoiCell(row, 2, c.getAddress(), s);
                ExportUtils.setPoiCell(row, 3, c.getState(),   s);
                ExportUtils.setPoiCell(row, 4, c.getCountry(), s);
                ExportUtils.setPoiCell(row, 5, c.getPincode(), s);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<CustomerResponseDto> customers) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont reg  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            ExportUtils.addPdfTitle(doc, "Customer Report", customers.size(), bold, reg);

            float[] widths = { 30f, 150f, 150f, 150f, 80f, 80f, 60f };
            Table table = new Table(UnitValue.createPointArray(widths)).useAllAvailableWidth();
            DeviceRgb hBg = new DeviceRgb(99, 102, 241);
            String[] cols = { "#", "Name", "Email", "Address", "State", "Country", "Pin" };
            for (String col : cols)
                table.addHeaderCell(ExportUtils.pdfHeaderCell(col, bold, hBg));

            DeviceRgb evenBg = new DeviceRgb(248, 249, 255);
            DeviceRgb white  = new DeviceRgb(255, 255, 255);
            for (int i = 0; i < customers.size(); i++) {
                CustomerResponseDto c = customers.get(i);
                DeviceRgb bg = (i % 2 == 0) ? evenBg : white;
                table.addCell(ExportUtils.pdfDataCell(String.valueOf(i + 1), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(c.getName(),    reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(c.getEmail(),   reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(c.getAddress(), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(c.getState(),   reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(c.getCountry(), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(c.getPincode(), reg, bg, true));
            }
            doc.add(table);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Customer Template");
            sheet.setDefaultColumnWidth(25);
            Row info = sheet.createRow(0);
            ExportUtils.setPoiCell(info, 0, "Fill customer data. * are required.",
                    ExportUtils.createInfoStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
            Row hRow = sheet.createRow(1);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < HEADERS.length; i++)
                ExportUtils.setPoiCell(hRow, i, HEADERS[i], hStyle);
            Row sRow = sheet.createRow(2);
            CellStyle sStyle = ExportUtils.createSampleStyle(wb);
            for (int i = 0; i < SAMPLE.length; i++)
                ExportUtils.setPoiCell(sRow, i, SAMPLE[i], sStyle);
            wb.write(out);
            return out.toByteArray();
        }
    }
}
