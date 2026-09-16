package com.ias.flashreserve.adapter.in.web;

import com.ias.flashreserve.adapter.in.web.dto.CreateReservationRequest;
import com.ias.flashreserve.adapter.in.web.dto.ReservationResponse;
import com.ias.flashreserve.domain.port.in.CreateReservationUseCase;
import com.ias.flashreserve.domain.port.in.GetReservationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;

    public ReservationController(
            CreateReservationUseCase createReservationUseCase,
            GetReservationUseCase getReservationUseCase
    ) {
        this.createReservationUseCase = createReservationUseCase;
        this.getReservationUseCase = getReservationUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ReservationResponse>> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return createReservationUseCase.create(idempotencyKey, request.clientId(), request.sku(), request.quantity())
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(saved)));
    }

    @GetMapping("/{id}")
    public Mono<ReservationResponse> get(@PathVariable String id) {
        return getReservationUseCase.byId(id).map(ReservationResponse::from);
    }
}
