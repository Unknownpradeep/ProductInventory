package com.hepl.product.Service.ServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.ProductDto.AttachmentDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;
import com.hepl.product.Repository.DivisionRepository;
import com.hepl.product.Repository.ProductAttachmentRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Service.ProductService;
import com.hepl.product.model.Division;
import com.hepl.product.model.Product;
import com.hepl.product.model.ProductAttachment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final ProductRepository repository;
    private final DivisionRepository divisionRepository;
    private final ProductAttachmentRepository attachmentRepository;

    @Override
    public Page<ProductResponseDto> listAll(String search, String code, Long divisionId, Double minPrice,
            Double maxPrice, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repository.searchAndFilter(search, code, divisionId, minPrice, maxPrice, pageable).map(this::mapToDto);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDto get(Long id) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));
        return mapToDto(p);
    }

    @Override
    public ProductResponseDto save(ProductRequestDto dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setPrice(dto.getPrice());
        p.setQuantity(dto.getQuantity());

        p.setCode("P" + System.currentTimeMillis());
        p.setSku(dto.getSku());
        p.setUom(dto.getUom());
        String bcodeSave = dto.getBatchcode() != null ? dto.getBatchcode().trim() : "";
        if (bcodeSave.equalsIgnoreCase("NA") || bcodeSave.equalsIgnoreCase("N/A") || bcodeSave.equalsIgnoreCase("NULL")) {
            bcodeSave = "";
        }
        p.setBatchcode(bcodeSave);
        p.setExpiryDate(dto.getExpiryDate());
        p.setGstpercentage(dto.getGstpercentage());
        p.setDiscount(dto.getDiscount());
        String divisionName = dto.getDivisionName() != null ? dto.getDivisionName().trim() : null;

        if (divisionName == null || divisionName.isBlank()) {
            throw new RuntimeException("Division name is required");
        }

        Division division = divisionRepository
                .findByNameIgnoreCase(divisionName)
                .orElseThrow(() -> new RuntimeException("Division not found"));
        p.setDivision(division);
        p.setDivisionName(division.getName());

        Product saved = repository.save(p);

        return mapToDto(saved);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        p.setName(dto.getName());
        p.setPrice(dto.getPrice());
        p.setQuantity(dto.getQuantity());
        p.setSku(dto.getSku());
        p.setUom(dto.getUom());
        String bcodeUpdate = dto.getBatchcode() != null ? dto.getBatchcode().trim() : "";
        if (bcodeUpdate.equalsIgnoreCase("NA") || bcodeUpdate.equalsIgnoreCase("N/A") || bcodeUpdate.equalsIgnoreCase("NULL")) {
            bcodeUpdate = "";
        }
        p.setBatchcode(bcodeUpdate);
        p.setExpiryDate(dto.getExpiryDate());
        p.setGstpercentage(dto.getGstpercentage());
        p.setDiscount(dto.getDiscount());
        String divisionName = dto.getDivisionName() != null ? dto.getDivisionName().trim() : null;

        if (divisionName == null || divisionName.isBlank()) {
            throw new RuntimeException("Division name is required");
        }

        Division division = divisionRepository
                .findByNameIgnoreCase(divisionName)
                .orElseThrow(() -> new RuntimeException("Division not found"));

        p.setDivision(division);
        p.setDivisionName(divisionName);

        Product updated = repository.save(p);

        return mapToDto(updated);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));
        existing.setDeleted(true);
        repository.save(existing);
    }

    @Override
    public List<ProductResponseDto> findByCategory(Long categoryId) {
        List<Product> p = repository.findByDivisionId(categoryId);
        return p.stream().map(this::mapToDto).toList();
    }

    @Override
    public List<ProductResponseDto> findByCustomer(Long customerId) {
        return List.of();
    }

    @Override
    public List<ProductResponseDto> saveAll(List<ProductRequestDto> dtos) {
        return dtos.stream().map(this::save).toList();
    }

    @Override
    public AttachmentDto addAttachment(Long productId, MultipartFile file) throws IOException {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf("."))
                : "";
        String storedName = UUID.randomUUID() + ext;

        Files.copy(file.getInputStream(), dir.resolve(storedName));

        ProductAttachment attachment = new ProductAttachment();
        attachment.setFileName(original != null ? original : storedName);
        attachment.setUrl(baseUrl + "/uploads/products/" + storedName);
        attachment.setMimeType(file.getContentType());
        attachment.setProduct(product);

        return mapToAttachmentDto(attachmentRepository.save(attachment));
    }

    @Override
    public List<AttachmentDto> getAttachments(Long productId) {
        if (!repository.existsById(productId))
            throw new RuntimeException("Product Not Found");
        return attachmentRepository.findByProductId(productId).stream().map(this::mapToAttachmentDto).toList();
    }

    @Override
    public void deleteAttachment(Long productId, Long attachmentId) {
        ProductAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment Not Found"));
        if (!attachment.getProduct().getId().equals(productId))
            throw new RuntimeException("Attachment does not belong to product");
        attachmentRepository.deleteById(attachmentId);
    }

    private AttachmentDto mapToAttachmentDto(ProductAttachment a) {
        AttachmentDto dto = new AttachmentDto();
        dto.setId(a.getId());
        dto.setFileName(a.getFileName());
        dto.setUrl(a.getUrl());
        dto.setMimeType(a.getMimeType());
        return dto;
    }

    private ProductResponseDto mapToDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        dto.setSku(product.getSku());
        dto.setUom(product.getUom());
        dto.setBatchcode(product.getBatchcode());
        dto.setExpiryDate(product.getExpiryDate());
        dto.setGstpercentage(product.getGstpercentage());
        dto.setDiscount(product.getDiscount());
        dto.setDivisionName(
                product.getDivision() != null
                        ? product.getDivision().getName()
                        : null);
        return dto;
    }
}
