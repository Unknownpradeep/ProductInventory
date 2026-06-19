package com.hepl.product.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;

public interface CustomerImportExportService {
    Map<String, Object> importFromExcel(MultipartFile file) throws IOException;
    byte[] exportToExcel(List<CustomerResponseDto> customers) throws IOException;
    byte[] exportToPdf(List<CustomerResponseDto> customers) throws IOException;
    byte[] generateExcelTemplate() throws IOException;
}
