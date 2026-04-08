package com.hepl.product.Service.ServiceImpl;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.DivisionDTO.DivisionRequestDto;
import com.hepl.product.Payload.Dto.DivisionDTO.DivisionResponseDto;
import com.hepl.product.Repository.DivisionRepository;
import com.hepl.product.Service.DivisionService;
import com.hepl.product.model.Division;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DivisionServiceImpl implements DivisionService {

    private final DivisionRepository repository;

    @Override
    public Page<DivisionResponseDto> listAll(String search, int page, int size, String sortBy, String sortDir) {
       Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
       Pageable pageable = PageRequest.of(page, size, sort);
       return repository.searchAndFilter(search, pageable).map(this::mapToDto);
    }

    @Override
    public DivisionResponseDto get(Long id) {
        Division division = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Division Not Found"));
        return mapToDto(division);
    }

    @Override
    public DivisionResponseDto save(DivisionRequestDto division) {
        Division d = new Division();
        d.setName(division.getName());
        Division saved = repository.save(d);
        return mapToDto(saved);
    }

    @Override
    public DivisionResponseDto update(Long id, DivisionRequestDto division) {
        Division existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Division Not Found"));
        existing.setName(division.getName());
        return mapToDto(repository.save(existing));
    }

    @Override
    public void delete(Long id) {
        Division existing=repository.findById(id).orElseThrow(()->new RuntimeException("Division Not Found"));
        existing.setDeleted(true);
        repository.save(existing);
    }

    private DivisionResponseDto mapToDto(Division division) {
        DivisionResponseDto dto = new DivisionResponseDto();
        dto.setId(division.getId());
        dto.setName(division.getName());
        return dto;
    }
}
