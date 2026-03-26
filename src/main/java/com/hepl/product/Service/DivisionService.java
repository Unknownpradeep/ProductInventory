package com.hepl.product.Service;

import java.util.List;

import com.hepl.product.Payload.Dto.DivisionDTO.DivisionRequestDto;
import com.hepl.product.Payload.Dto.DivisionDTO.DivisionResponseDto;


public interface DivisionService {
    List<DivisionResponseDto> listAll();
    DivisionResponseDto get(Long id);
    DivisionResponseDto save(DivisionRequestDto division);
    DivisionResponseDto update(Long id, DivisionRequestDto division);
    void delete(Long id);
}
