package com.hepl.product.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.UserDto.UserResponseDto;


public interface UserImportExportService {
    Map<String, Object> importFromExcel(MultipartFile file) throws IOException;
    byte[] exportToExcel(List<UserResponseDto> users) throws IOException;
    byte[] exportToPdf(List<UserResponseDto> users) throws IOException;
    byte[] generateExcelTemplate() throws IOException;
}
