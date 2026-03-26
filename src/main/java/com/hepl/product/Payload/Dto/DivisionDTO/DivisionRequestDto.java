package com.hepl.product.Payload.Dto.DivisionDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DivisionRequestDto {
    @NotBlank(message = "Name is required")
    private String name;
}
