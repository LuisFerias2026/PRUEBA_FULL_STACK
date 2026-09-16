package com.ias.flashreserve.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotBlank(message = "clientId is required") String clientId,
        @NotBlank(message = "sku is required") String sku,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be between 1 and 5")
        @Max(value = 5, message = "quantity must be between 1 and 5")
        Integer quantity
) {
}
