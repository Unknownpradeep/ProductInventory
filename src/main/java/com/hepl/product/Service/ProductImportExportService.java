package com.hepl.product.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;

public interface ProductImportExportService {

    /** Parse an uploaded .xlsx file and return imported products with row-level results */
    Map<String, Object> importFromExcel(MultipartFile file) throws IOException;

    /** Export all products to an .xlsx byte array */
    byte[] exportToExcel(List<ProductResponseDto> products) throws IOException;

    /** Export all products to a PDF byte array */
    byte[] exportToPdf(List<ProductResponseDto> products) throws IOException;

    /** Generate a downloadable Excel template (empty with headers + sample row) */
    byte[] generateExcelTemplate() throws IOException;
}
