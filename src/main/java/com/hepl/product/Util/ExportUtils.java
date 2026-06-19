package com.hepl.product.Util;

import java.time.LocalDate;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.Table;

public class ExportUtils {

    // ─── POI Styles ───────────────────────────────────────────────────────────
    
    public static CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    public static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    public static CellStyle createEvenRowStyle(Workbook wb) {
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

    public static CellStyle createOddRowStyle(Workbook wb) {
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

    public static CellStyle createInfoStyle(Workbook wb) {
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

    public static CellStyle createSampleStyle(Workbook wb) {
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

    public static void setPoiCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    public static String getCellStr(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { yield cell.getStringCellValue().trim(); }
            }
            default -> "";
        };
    }

    // ─── iText Helpers ────────────────────────────────────────────────────────
    
    public static void addPdfTitle(Document doc, String title, int count, PdfFont boldFont, PdfFont regularFont) {
        DeviceRgb titleColor = new DeviceRgb(60, 60, 120);
        doc.add(new Paragraph(title)
                .setFont(boldFont).setFontSize(20)
                .setFontColor(titleColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        doc.add(new Paragraph("Generated: " + LocalDate.now()
                + "   |   Total Records: " + count)
                .setFont(regularFont).setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(16));
    }

    public static com.itextpdf.layout.element.Cell pdfHeaderCell(String text, PdfFont font, DeviceRgb bgColor) {
        String val = text != null ? text : "";
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(val).setFont(font).setFontSize(8))
                .setBackgroundColor(bgColor)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }

    public static com.itextpdf.layout.element.Cell pdfDataCell(String text, PdfFont font, DeviceRgb rowBg, boolean center) {
        String val = text != null ? text : "";
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(val).setFont(font).setFontSize(8))
                .setBackgroundColor(rowBg)
                .setTextAlignment(center ? TextAlignment.CENTER : TextAlignment.LEFT)
                .setPadding(5);
    }
}
