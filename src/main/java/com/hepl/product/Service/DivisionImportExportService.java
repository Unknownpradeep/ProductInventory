package com.hepl.product.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.DivisionDTO.DivisionResponseDto;

public interface DivisionImportExportService {
    Map<String, Object> importFromExcel(MultipartFile file) throws IOException;
    byte[] exportToExcel(List<DivisionResponseDto> divisions) throws IOException;
    byte[] exportToPdf(List<DivisionResponseDto> divisions) throws IOException;
    byte[] generateExcelTemplate() throws IOException;
}
