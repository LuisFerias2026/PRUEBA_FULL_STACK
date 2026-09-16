package com.ias.flashreserve.adapter.in.provider;

import com.ias.flashreserve.adapter.in.web.ApiException;
import com.ias.flashreserve.adapter.in.web.dto.ProviderEventRequest;
import com.ias.flashreserve.adapter.in.web.dto.ProviderEventResponse;
import com.ias.flashreserve.domain.model.ProviderCommand;
import com.ias.flashreserve.domain.port.in.ApplyProviderEventUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/provider/events")
public class ProviderEventController {

    private final HmacSignatureValidator hmacSignatureValidator;
    private final ApplyProviderEventUseCase applyProviderEventUseCase;
    private final ObjectMapper objectMapper;

    public ProviderEventController(
            HmacSignatureValidator hmacSignatureValidator,
            ApplyProviderEventUseCase applyProviderEventUseCase,
            ObjectMapper objectMapper
    ) {
        this.hmacSignatureValidator = hmacSignatureValidator;
        this.applyProviderEventUseCase = applyProviderEventUseCase;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Mono<ProviderEventResponse> ingest(
            @RequestHeader(value = "X-Provider-Signature", required = false) String signature,
            @RequestBody String rawBody
    ) {
        if (!hmacSignatureValidator.isValid(signature, rawBody)) {
            return Mono.error(ApiException.unauthorized(
                    "INVALID_SIGNATURE",
                    "Provider signature is missing or invalid."
            ));
        }
        ProviderEventRequest request;
        try {
            request = objectMapper.readValue(rawBody, ProviderEventRequest.class);
        } catch (JsonProcessingException e) {
            return Mono.error(ApiException.badRequest("INVALID_BODY", "Provider event body is invalid JSON."));
        }
        long sequence = request.sequence() == null ? 0 : request.sequence();
        ProviderCommand command = new ProviderCommand(
                request.eventId(),
                request.reservationId(),
                sequence,
                request.status(),
                request.occurredAt()
        );
        return applyProviderEventUseCase.apply(command).map(ProviderEventResponse::from);
    }
}
