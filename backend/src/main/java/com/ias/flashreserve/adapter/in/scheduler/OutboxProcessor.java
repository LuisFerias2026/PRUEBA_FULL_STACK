package com.ias.flashreserve.adapter.in.scheduler;

import com.ias.flashreserve.config.AppProperties;
import com.ias.flashreserve.domain.model.OutboxMessage;
import com.ias.flashreserve.domain.model.OutboxStatus;
import com.ias.flashreserve.domain.port.out.AuditPublisher;
import com.ias.flashreserve.domain.port.out.OutboxStore;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxStore outboxStore;
    private final AuditPublisher auditPublisher;
    private final AppProperties properties;
    private Disposable subscription;

    public OutboxProcessor(OutboxStore outboxStore, AuditPublisher auditPublisher, AppProperties properties) {
        this.outboxStore = outboxStore;
        this.auditPublisher = auditPublisher;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.getOutbox().isEnabled()) {
            log.info("outbox_processor_disabled");
            return;
        }
        Duration interval = Duration.ofMillis(properties.getOutbox().getPollIntervalMs());
        subscription = Flux.interval(interval)
                .flatMap(tick -> processBatch())
                .onErrorContinue((error, item) -> log.warn("outbox_processor_tick_failed error={}", error.toString()))
                .subscribe();
        log.info("outbox_processor_started intervalMs={}", properties.getOutbox().getPollIntervalMs());
    }

    private Mono<Void> processBatch() {
        return outboxStore.findPending()
                .concatMap(this::publish)
                .then();
    }

    private Mono<Void> publish(OutboxMessage message) {
        return auditPublisher.publish(message)
                .then(Mono.defer(() -> outboxStore.save(new OutboxMessage(
                        message.id(),
                        message.type(),
                        message.aggregateId(),
                        message.payload(),
                        OutboxStatus.SENT,
                        message.attempts() + 1,
                        message.createdAt(),
                        Instant.now(),
                        null
                ))))
                .then()
                .onErrorResume(error -> {
                    log.warn("audit_notification_failed id={} error={}", message.id(), error.toString());
                    return outboxStore.save(new OutboxMessage(
                            message.id(),
                            message.type(),
                            message.aggregateId(),
                            message.payload(),
                            OutboxStatus.FAILED,
                            message.attempts() + 1,
                            message.createdAt(),
                            Instant.now(),
                            error.toString()
                    )).then();
                });
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
