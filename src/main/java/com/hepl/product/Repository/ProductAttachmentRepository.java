package com.hepl.product.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hepl.product.model.ProductAttachment;

public interface ProductAttachmentRepository extends JpaRepository<ProductAttachment, Long> {
    List<ProductAttachment> findByProductId(Long productId);
}
