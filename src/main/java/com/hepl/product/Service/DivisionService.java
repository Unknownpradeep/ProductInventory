package com.hepl.product.Service;

import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.DivisionDTO.DivisionRequestDto;
import com.hepl.product.Payload.Dto.DivisionDTO.DivisionResponseDto;

public interface DivisionService {
    Page<DivisionResponseDto> listAll(String search, int page, int size, String sortBy, String sortDir);
    DivisionResponseDto get(Long id);
    DivisionResponseDto save(DivisionRequestDto division);
    DivisionResponseDto update(Long id, DivisionRequestDto division);
    void delete(Long id);
}
