package com.hepl.product.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "product_attachment")
public class ProductAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String url;
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
