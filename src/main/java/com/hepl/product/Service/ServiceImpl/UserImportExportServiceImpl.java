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
import com.hepl.product.Payload.Dto.UserDto.UserResponseDto;
import com.hepl.product.Payload.Dto.UserDto.UserRequestDto;
import com.hepl.product.Service.UserImportExportService;
import com.hepl.product.Service.UserService;
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
public class UserImportExportServiceImpl implements UserImportExportService {

    private final UserService userService;
    private final com.hepl.product.Repository.UserRepository userRepository;

    private static final String[] HEADERS = { "Username*", "First Name*", "Last Name", "Email*", "Phone", "Status" };
    private static final String[] SAMPLE = { "johnd", "John", "Doe", "john@example.com", "9876543210", "ACTIVE" };

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();
            if (rowIter.hasNext())
                rowIter.next(); // skip info
            if (rowIter.hasNext())
                rowIter.next(); // skip headers

            int rowNum = 3;
            Set<String> seenUsernamesInSheet = new HashSet<>();
            Set<String> seenEmailsInSheet = new HashSet<>();

            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", rowNum++);

                try {
                    String username = ExportUtils.getCellStr(row, 0).trim();
                    String firstName = ExportUtils.getCellStr(row, 1).trim();
                    String lastName = ExportUtils.getCellStr(row, 2).trim();
                    String email = ExportUtils.getCellStr(row, 3).trim();
                    String phone = ExportUtils.getCellStr(row, 4).trim();
                    String status = ExportUtils.getCellStr(row, 5).trim();

                    if (username.isBlank() && email.isBlank())
                        continue;

                    List<String> errors = new ArrayList<>();
                    if (username.isBlank())
                        errors.add("Username is required");
                    if (firstName.isBlank())
                        errors.add("First Name is required");
                    if (email.isBlank()) {
                        errors.add("Email is required");
                    } else if (!email.contains("@")) {
                        errors.add("Invalid email format");
                    }

                    if (errors.isEmpty()) {
                        String normUsername = username.toLowerCase().trim();
                        if (seenUsernamesInSheet.contains(normUsername)) {
                            errors.add("Duplicate username found in this Excel sheet");
                        } else {
                            seenUsernamesInSheet.add(normUsername);
                        }

                        String normEmail = email.toLowerCase().trim();
                        if (seenEmailsInSheet.contains(normEmail)) {
                            errors.add("Duplicate email found in this Excel sheet");
                        } else {
                            seenEmailsInSheet.add(normEmail);
                        }
                    }

                    if (errors.isEmpty()) {
                        List<com.hepl.product.model.User> usersByUsername = userRepository.findAllByUsernameIgnoreCase(username);
                        boolean usernameExistsActive = usersByUsername.stream().anyMatch(u -> !u.isDeleted());
                        if (usernameExistsActive) {
                            errors.add("A user with this username already exists");
                        }

                        List<com.hepl.product.model.User> usersByEmail = userRepository.findAllByEmailIgnoreCase(email);
                        boolean emailExistsActive = usersByEmail.stream().anyMatch(u -> !u.isDeleted());
                        if (emailExistsActive) {
                            errors.add("A user with this email already exists");
                        }
                    }

                    if (!errors.isEmpty()) {
                        rowResult.put("status", "FAILED");
                        rowResult.put("errors", errors);
                        results.add(rowResult);
                        failed++;
                        continue;
                    }

                    UserRequestDto dto = new UserRequestDto();
                    dto.setUsername(username);
                    dto.setFirstname(firstName);
                    dto.setLastname(lastName);
                    dto.setEmail(email);
                    if (!phone.isBlank())
                        dto.setPhonenumber(Long.parseLong(phone));
                    dto.setStatus(status.isBlank() ? "ACTIVE" : status);
                    // Passwords should be handled by UserService or a default assigned
                    dto.setPassword("Welcome@123");

                    UserResponseDto saved = userService.save(dto);
                    rowResult.put("status", "SUCCESS");
                    rowResult.put("username", saved.getUsername());
                    rowResult.put("id", saved.getId());
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
    public byte[] exportToExcel(List<UserResponseDto> users) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Users");
            sheet.setDefaultColumnWidth(20);

            String[] exportHeaders = HEADERS;

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            ExportUtils.setPoiCell(titleRow, 0, "User Directory Export | " + LocalDate.now(),
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
            for (int i = 0; i < users.size(); i++) {
                UserResponseDto u = users.get(i);
                Row row = sheet.createRow(i + 2);
                CellStyle s = (i % 2 == 0) ? even : odd;
                ExportUtils.setPoiCell(row, 0, u.getUsername(), s);
                ExportUtils.setPoiCell(row, 1, u.getFirstname(), s);
                ExportUtils.setPoiCell(row, 2, u.getLastname() != null ? u.getLastname() : "", s);
                ExportUtils.setPoiCell(row, 3, u.getEmail(), s);
                ExportUtils.setPoiCell(row, 4, u.getPhonenumber() != null ? String.valueOf(u.getPhonenumber()) : "", s);
                ExportUtils.setPoiCell(row, 5, u.getStatus(), s);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<UserResponseDto> users) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            ExportUtils.addPdfTitle(doc, "User Management Report", users.size(), bold, reg);

            float[] widths = { 30f, 100f, 150f, 150f, 100f, 60f };
            Table table = new Table(UnitValue.createPointArray(widths)).useAllAvailableWidth();
            DeviceRgb hBg = new DeviceRgb(99, 102, 241);
            String[] cols = { "#", "Username", "Name", "Email", "Phone", "Status" };
            for (String col : cols)
                table.addHeaderCell(ExportUtils.pdfHeaderCell(col, bold, hBg));

            DeviceRgb evenBg = new DeviceRgb(248, 249, 255);
            DeviceRgb white = new DeviceRgb(255, 255, 255);
            for (int i = 0; i < users.size(); i++) {
                UserResponseDto u = users.get(i);
                DeviceRgb bg = (i % 2 == 0) ? evenBg : white;
                table.addCell(ExportUtils.pdfDataCell(String.valueOf(i + 1), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(u.getUsername(), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(
                        u.getFirstname() + " " + (u.getLastname() != null ? u.getLastname() : ""), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(u.getEmail(), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(
                        u.getPhonenumber() != null ? String.valueOf(u.getPhonenumber()) : "-", reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(u.getStatus(), reg, bg, true));
            }
            doc.add(table);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("User Template");
            sheet.setDefaultColumnWidth(20);
            Row info = sheet.createRow(0);
            ExportUtils.setPoiCell(info, 0, "Fill user data. * are required. Password defaults to Welcome@123",
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
