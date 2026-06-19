package com.hepl.product.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.hepl.product.model.Order;

public interface OrderImportExportService {
    Map<String, Object> importFromExcel(MultipartFile file) throws IOException;
    byte[] exportToExcel(List<Order> orders) throws IOException;
    byte[] exportToPdf(List<Order> orders) throws IOException;
    byte[] generateExcelTemplate() throws IOException;
}
