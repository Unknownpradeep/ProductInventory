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
import com.hepl.product.Payload.Dto.DivisionDTO.DivisionRequestDto;
import com.hepl.product.Payload.Dto.DivisionDTO.DivisionResponseDto;
import com.hepl.product.Service.DivisionImportExportService;
import com.hepl.product.Service.DivisionService;
import com.hepl.product.Repository.DivisionRepository;
import com.hepl.product.model.Division;
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
public class DivisionImportExportServiceImpl implements DivisionImportExportService {

    private final DivisionService divisionService;
    private final DivisionRepository divisionRepository;

    private static final String[] HEADERS = { "Name*" };
    private static final String[] SAMPLE = { "Electronics" };

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
            // To track duplicate names within the same Excel sheet in a case-insensitive
            // manner
            Set<String> seenNamesInSheet = new HashSet<>();

            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", rowNum++);

                try {
                    String name = ExportUtils.getCellStr(row, 0).trim();

                    if (name.isBlank())
                        continue;

                    List<String> errors = new ArrayList<>();
                    if (name.isBlank()) {
                        errors.add("Name is required");
                    } else if (name.length() < 2) {
                        errors.add("Name must be at least 2 characters");
                    } else if (!name.matches("^[a-zA-Z0-9\\s&\\-_.]+$")) {
                        errors.add("Name contains invalid characters");
                    }

                    // Check for duplicate in the Excel itself
                    String normName = name.toLowerCase();
                    if (seenNamesInSheet.contains(normName)) {
                        errors.add("Duplicate division name found in this Excel sheet");
                    } else {
                        seenNamesInSheet.add(normName);
                    }

                    // Check for duplicate in the database
                    if (errors.isEmpty()) {
                        List<Division> divs = divisionRepository.findAllByNameIgnoreCase(name);
                        boolean existsActive = divs.stream().anyMatch(d -> !d.isDeleted());
                        if (existsActive) {
                            errors.add("A division with this name already exists");
                        }
                    }

                    if (!errors.isEmpty()) {
                        rowResult.put("status", "FAILED");
                        rowResult.put("name", name);
                        rowResult.put("errors", errors);
                        results.add(rowResult);
                        failed++;
                        continue;
                    }

                    // Save or Reactivate
                    List<Division> divs = divisionRepository.findAllByNameIgnoreCase(name);
                    DivisionResponseDto savedDto;
                    Division existingDiv = divs.stream()
                            .filter(d -> d.isDeleted())
                            .findFirst()
                            .orElse(divs.isEmpty() ? null : divs.get(0));
                    if (existingDiv != null) {
                        existingDiv.setDeleted(false);
                        Division saved = divisionRepository.save(existingDiv);
                        savedDto = new DivisionResponseDto();
                        savedDto.setId(saved.getId());
                        savedDto.setName(saved.getName());
                        savedDto.setBatchcode(saved.getBatchcode());
                    } else {
                        DivisionRequestDto dto = new DivisionRequestDto();
                        dto.setName(name);
                        savedDto = divisionService.save(dto);
                    }

                    rowResult.put("status", "SUCCESS");
                    rowResult.put("name", savedDto.getName());
                    rowResult.put("id", savedDto.getId());
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
    public byte[] exportToExcel(List<DivisionResponseDto> divisions) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Divisions");
            sheet.setDefaultColumnWidth(25);

            String[] exportHeaders = HEADERS;

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            ExportUtils.setPoiCell(titleRow, 0, "Division Directory Export | " + LocalDate.now(),
                    ExportUtils.createTitleStyle(wb));
            if (exportHeaders.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, exportHeaders.length - 1));
            }

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            CellStyle hStyle = ExportUtils.createHeaderStyle(wb);
            for (int i = 0; i < exportHeaders.length; i++) {
                ExportUtils.setPoiCell(headerRow, i, exportHeaders[i], hStyle);
            }

            CellStyle even = ExportUtils.createEvenRowStyle(wb);
            CellStyle odd = ExportUtils.createOddRowStyle(wb);
            for (int i = 0; i < divisions.size(); i++) {
                DivisionResponseDto d = divisions.get(i);
                Row row = sheet.createRow(i + 2);
                CellStyle s = (i % 2 == 0) ? even : odd;
                ExportUtils.setPoiCell(row, 0, d.getName(), s);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<DivisionResponseDto> divisions) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            ExportUtils.addPdfTitle(doc, "Division Report", divisions.size(), bold, reg);

            float[] widths = { 50f, 150f, 250f };
            Table table = new Table(UnitValue.createPointArray(widths)).useAllAvailableWidth();
            DeviceRgb hBg = new DeviceRgb(99, 102, 241);
            String[] cols = { "#", "Batch Code", "Name" };
            for (String col : cols)
                table.addHeaderCell(ExportUtils.pdfHeaderCell(col, bold, hBg));

            DeviceRgb evenBg = new DeviceRgb(248, 249, 255);
            DeviceRgb white = new DeviceRgb(255, 255, 255);
            for (int i = 0; i < divisions.size(); i++) {
                DivisionResponseDto d = divisions.get(i);
                DeviceRgb bg = (i % 2 == 0) ? evenBg : white;
                table.addCell(ExportUtils.pdfDataCell(String.valueOf(i + 1), reg, bg, true));
                table.addCell(ExportUtils.pdfDataCell(d.getBatchcode(), reg, bg, false));
                table.addCell(ExportUtils.pdfDataCell(d.getName(), reg, bg, false));
            }
            doc.add(table);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Division Template");
            sheet.setDefaultColumnWidth(25);
            Row info = sheet.createRow(0);
            ExportUtils.setPoiCell(info, 0, "Fill division data. * are required.", ExportUtils.createInfoStyle(wb));
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
