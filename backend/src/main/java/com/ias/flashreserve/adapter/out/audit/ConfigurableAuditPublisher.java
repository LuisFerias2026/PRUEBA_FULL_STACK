package com.ias.flashreserve.adapter.out.audit;

import com.ias.flashreserve.config.AppProperties;
import com.ias.flashreserve.domain.model.OutboxMessage;
import com.ias.flashreserve.domain.port.out.AuditPublisher;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.Map;

@Component
public class ConfigurableAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableAuditPublisher.class);

    private final AppProperties properties;
    private volatile SqsAsyncClient sqsClient;

    public ConfigurableAuditPublisher(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> publish(OutboxMessage message) {
        String queueUrl = properties.getOutbox().getSqsQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            log.info(
                    "audit_notification type={} aggregateId={} payload={}",
                    message.type(),
                    message.aggregateId(),
                    message.payload()
            );
            return Mono.empty();
        }
        return sendToSqs(message, queueUrl.trim());
    }

    private Mono<Void> sendToSqs(OutboxMessage message, String queueUrl) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message.payload())
                .messageAttributes(Map.of(
                        "type", stringAttr(message.type()),
                        "aggregateId", stringAttr(message.aggregateId()),
                        "outboxId", stringAttr(message.id())
                ))
                .build();
        return Mono.fromFuture(() -> client().sendMessage(request))
                .doOnNext(ignored -> log.info(
                        "audit_notification_sqs type={} aggregateId={} queue={}",
                        message.type(),
                        message.aggregateId(),
                        queueUrl
                ))
                .then();
    }

    private SqsAsyncClient client() {
        SqsAsyncClient existing = sqsClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (sqsClient == null) {
                sqsClient = buildClient();
            }
            return sqsClient;
        }
    }

    private SqsAsyncClient buildClient() {
        AppProperties.Outbox outbox = properties.getOutbox();
        var builder = SqsAsyncClient.builder().region(Region.of(outbox.getSqsRegion()));
        String endpoint = outbox.getSqsEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")
                    ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    private static MessageAttributeValue stringAttr(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value).build();
    }

    @PreDestroy
    public void close() {
        SqsAsyncClient client = sqsClient;
        if (client != null) {
            client.close();
        }
    }
}
