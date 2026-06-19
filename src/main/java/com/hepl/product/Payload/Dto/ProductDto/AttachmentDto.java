package com.hepl.product.Payload.Dto.ProductDto;

import java.io.Serializable;

import lombok.Data;

@Data
public class AttachmentDto implements Serializable {
    private Long id;
    private String fileName;
    private String url;
    private String mimeType;
}
