package com.hepl.product.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.hepl.product.model.Stock;

public interface StockImportExportService {
    Map<String, Object> importFromExcel(MultipartFile file) throws IOException;
    byte[] exportToExcel(List<Stock> stocks) throws IOException;
    byte[] exportToPdf(List<Stock> stocks) throws IOException;
    byte[] generateExcelTemplate() throws IOException;
}
