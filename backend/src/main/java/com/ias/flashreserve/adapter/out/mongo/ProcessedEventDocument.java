package com.ias.flashreserve.adapter.out.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "processed_events")
public class ProcessedEventDocument {

    @Id
    private String eventId;
    @Indexed
    private String reservationId;
    private long sequence;
    private Instant processedAt;

    public ProcessedEventDocument() {
    }

    public ProcessedEventDocument(String eventId, String reservationId, long sequence, Instant processedAt) {
        this.eventId = eventId;
        this.reservationId = reservationId;
        this.sequence = sequence;
        this.processedAt = processedAt;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
