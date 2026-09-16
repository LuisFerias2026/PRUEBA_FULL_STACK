package com.ias.flashreserve;

import com.ias.flashreserve.adapter.in.provider.HmacSignatureValidator;
import com.ias.flashreserve.adapter.in.web.dto.ProviderEventResponse;
import com.ias.flashreserve.adapter.in.web.dto.ReservationResponse;
import com.ias.flashreserve.domain.model.InventoryItem;
import com.ias.flashreserve.domain.port.out.InventoryStore;
import com.ias.flashreserve.domain.port.out.OutboxStore;
import com.ias.flashreserve.domain.port.out.ProcessedEventStore;
import com.ias.flashreserve.domain.port.out.ReservationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.outbox.enabled=false",
        "app.provider.hmac-secret=test-secret",
        "app.demo.enabled=true",
        "de.flapdoodle.mongodb.embedded.version=7.0.12"
})
class ReservationFlowTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private InventoryStore inventoryStore;

    @Autowired
    private ReservationStore reservationStore;

    @Autowired
    private ProcessedEventStore processedEventStore;

    @Autowired
    private OutboxStore outboxStore;

    @Autowired
    private HmacSignatureValidator hmacSignatureValidator;

    @LocalServerPort
    private int port;

    @SuppressWarnings("null")
@BeforeEach
    void resetData() {
        reservationStore.deleteAll().block();
        processedEventStore.deleteAll().block();
        outboxStore.deleteAll().block();
        inventoryStore.deleteAll().block();
        inventoryStore.save(new InventoryItem("SKU-FLASH-A", "Flash campaign A", 10)).block();
        inventoryStore.save(new InventoryItem("SKU-FLASH-B", "Flash campaign B", 3)).block();
        inventoryStore.save(new InventoryItem("SKU-CONC", "Concurrency sku", 1)).block();
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(15)).build();
    }

    @Test
    void createsReservationAndDecrementsStock() {
        ReservationResponse created = createReservation("client-1", "SKU-FLASH-A", 2, UUID.randomUUID().toString());

        assertThat(created.status().name()).isEqualTo("PENDING");
        assertThat(created.quantity()).isEqualTo(2);
        assertThat(available("SKU-FLASH-A")).isEqualTo(8);

        webTestClient.get()
                .uri("/api/reservations/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ReservationResponse.class)
                .value(found -> assertThat(found.id()).isEqualTo(created.id()));
    }

    @Test
    void repeatedIdempotencyKeyDoesNotDuplicateBusinessEffect() {
        String key = "idem-" + UUID.randomUUID();
        ReservationResponse first = createReservation("client-1", "SKU-FLASH-A", 2, key);
        ReservationResponse second = createReservation("client-1", "SKU-FLASH-A", 2, key);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(available("SKU-FLASH-A")).isEqualTo(8);
        StepVerifier.create(reservationStore.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @SuppressWarnings("null")
@Test
    void rejectsInvalidQuantity() {
        webTestClient.post()
                .uri("/api/reservations")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"c1\",\"sku\":\"SKU-FLASH-A\",\"quantity\":6}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @SuppressWarnings("null")
@Test
    void rejectsWhenStockIsInsufficient() {
        webTestClient.post()
                .uri("/api/reservations")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"c1\",\"sku\":\"SKU-FLASH-B\",\"quantity\":5}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(available("SKU-FLASH-B")).isEqualTo(3);
    }

    @Test
    void duplicateOrStaleProviderEventsDoNotRevertState() {
        ReservationResponse created = createReservation("client-1", "SKU-FLASH-A", 1, UUID.randomUUID().toString());

        String confirmBody = eventBody("evt-confirm", created.id(), 2, "CONFIRMED");
        postProviderEvent(confirmBody)
                .expectStatus().isOk()
                .expectBody(ProviderEventResponse.class)
                .value(response -> assertThat(response.outcome()).isEqualTo("APPLIED"));

        webTestClient.get()
                .uri("/api/reservations/{id}", created.id())
                .exchange()
                .expectBody(ReservationResponse.class)
                .value(found -> {
                    assertThat(found.status().name()).isEqualTo("CONFIRMED");
                    assertThat(found.lastSequence()).isEqualTo(2);
                });

        postProviderEvent(confirmBody)
                .expectStatus().isOk()
                .expectBody(ProviderEventResponse.class)
                .value(response -> assertThat(response.outcome()).isEqualTo("IGNORED_DUPLICATE"));

        String staleReject = eventBody("evt-stale", created.id(), 1, "REJECTED");
        postProviderEvent(staleReject)
                .expectStatus().isOk()
                .expectBody(ProviderEventResponse.class)
                .value(response -> assertThat(response.outcome()).isEqualTo("IGNORED_STALE_SEQUENCE"));

        webTestClient.get()
                .uri("/api/reservations/{id}", created.id())
                .exchange()
                .expectBody(ReservationResponse.class)
                .value(found -> assertThat(found.status().name()).isEqualTo("CONFIRMED"));
    }

    @SuppressWarnings("null")
@Test
    void rejectsUnsignedProviderEvent() {
        ReservationResponse created = createReservation("client-1", "SKU-FLASH-A", 1, UUID.randomUUID().toString());
        String body = eventBody("evt-unsigned", created.id(), 1, "CONFIRMED");
        webTestClient.post()
                .uri("/api/provider/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_SIGNATURE");
    }

    @Test
    void concurrentReservationsOnSameSkuDoNotOversell() {
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        Mono<Integer> first = reserveStatus(client, "SKU-CONC", UUID.randomUUID().toString());
        Mono<Integer> second = reserveStatus(client, "SKU-CONC", UUID.randomUUID().toString());

        List<Integer> statuses = Mono.zip(first, second)
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()))
                .block(Duration.ofSeconds(20));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(available("SKU-CONC")).isEqualTo(0);
        StepVerifier.create(reservationStore.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void rejectedProviderEventRestoresStockOnce() {
        ReservationResponse created = createReservation("client-1", "SKU-FLASH-A", 2, UUID.randomUUID().toString());
        assertThat(available("SKU-FLASH-A")).isEqualTo(8);

        String rejectBody = eventBody("evt-reject", created.id(), 1, "REJECTED");
        postProviderEvent(rejectBody)
                .expectStatus().isOk()
                .expectBody(ProviderEventResponse.class)
                .value(response -> assertThat(response.outcome()).isEqualTo("APPLIED"));

        assertThat(available("SKU-FLASH-A")).isEqualTo(10);

        String again = eventBody("evt-reject-2", created.id(), 2, "REJECTED");
        postProviderEvent(again)
                .expectStatus().isOk()
                .expectBody(ProviderEventResponse.class)
                .value(response -> assertThat(response.outcome()).isEqualTo("APPLIED"));

        assertThat(available("SKU-FLASH-A")).isEqualTo(10);
    }

    @Test
    void confirmedProviderEventDoesNotRestoreStock() {
        ReservationResponse created = createReservation("client-1", "SKU-FLASH-A", 2, UUID.randomUUID().toString());
        String confirmBody = eventBody("evt-keep-stock", created.id(), 1, "CONFIRMED");
        postProviderEvent(confirmBody).expectStatus().isOk();
        assertThat(available("SKU-FLASH-A")).isEqualTo(8);
    }

    @SuppressWarnings("null")
@Test
    void demoProviderEndpointAppliesWithoutClientHmac() {
        ReservationResponse created = createReservation("client-1", "SKU-FLASH-A", 1, UUID.randomUUID().toString());
        webTestClient.post()
                .uri("/api/demo/provider-events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(eventBody("evt-demo", created.id(), 1, "CONFIRMED"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProviderEventResponse.class)
                .value(response -> assertThat(response.outcome()).isEqualTo("APPLIED"));
    }

    @SuppressWarnings("null")
private ReservationResponse createReservation(String clientId, String sku, int quantity, String key) {
        return webTestClient.post()
                .uri("/api/reservations")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"" + clientId + "\",\"sku\":\"" + sku + "\",\"quantity\":" + quantity + "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ReservationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @SuppressWarnings("null")
private WebTestClient.ResponseSpec postProviderEvent(String body) {
        return webTestClient.post()
                .uri("/api/provider/events")
                .header("X-Provider-Signature", hmacSignatureValidator.sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private String eventBody(String eventId, String reservationId, long sequence, String status) {
        return "{\"eventId\":\"" + eventId
                + "\",\"reservationId\":\"" + reservationId
                + "\",\"sequence\":" + sequence
                + ",\"status\":\"" + status
                + "\",\"occurredAt\":\"2026-09-16T12:00:00Z\"}";
    }

    @SuppressWarnings("null")
private int available(String sku) {
        return inventoryStore.findBySku(sku)
                .map(InventoryItem::available)
                .block();
    }

    @SuppressWarnings("null")
private Mono<Integer> reserveStatus(WebClient client, String sku, String key) {
        return client.post()
                .uri("/api/reservations")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"race\",\"sku\":\"" + sku + "\",\"quantity\":1}")
                .exchangeToMono(response -> Mono.just(response.statusCode().value()));
    }
}
