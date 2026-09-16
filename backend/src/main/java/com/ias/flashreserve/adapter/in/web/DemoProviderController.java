package com.ias.flashreserve.adapter.in.web;

import com.ias.flashreserve.adapter.in.web.dto.ProviderEventRequest;
import com.ias.flashreserve.adapter.in.web.dto.ProviderEventResponse;
import com.ias.flashreserve.domain.model.ProviderCommand;
import com.ias.flashreserve.domain.port.in.ApplyProviderEventUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@RequestMapping("/api/demo/provider-events")
public class DemoProviderController {

    private final ApplyProviderEventUseCase applyProviderEventUseCase;

    public DemoProviderController(ApplyProviderEventUseCase applyProviderEventUseCase) {
        this.applyProviderEventUseCase = applyProviderEventUseCase;
    }

    @PostMapping
    public Mono<ProviderEventResponse> ingest(@RequestBody ProviderEventRequest request) {
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
