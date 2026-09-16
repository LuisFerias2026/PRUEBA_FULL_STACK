package com.ias.flashreserve.adapter.in.web.dto;

import com.ias.flashreserve.domain.model.ProviderEventResult;

public record ProviderEventResponse(
        String eventId,
        String outcome,
        String reservationId,
        String status
) {
    public static ProviderEventResponse from(ProviderEventResult result) {
        return new ProviderEventResponse(
                result.eventId(),
                result.outcome().name(),
                result.reservationId(),
                result.status() == null ? null : result.status().name()
        );
    }
}
