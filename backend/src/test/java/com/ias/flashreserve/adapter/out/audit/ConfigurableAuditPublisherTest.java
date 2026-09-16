package com.ias.flashreserve.adapter.out.audit;

import com.ias.flashreserve.config.AppProperties;
import com.ias.flashreserve.domain.model.OutboxMessage;
import com.ias.flashreserve.domain.model.OutboxStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;

class ConfigurableAuditPublisherTest {

    @Test
    void completesWithoutSqsWhenQueueUrlIsBlank() {
        AppProperties properties = new AppProperties();
        ConfigurableAuditPublisher publisher = new ConfigurableAuditPublisher(properties);
        OutboxMessage message = new OutboxMessage(
                "obx_1",
                "RESERVATION_CREATED",
                "res_1",
                "{\"reservationId\":\"res_1\"}",
                OutboxStatus.PENDING,
                0,
                Instant.parse("2026-09-16T12:00:00Z"),
                null,
                null
        );

        StepVerifier.create(publisher.publish(message)).verifyComplete();
    }
}
