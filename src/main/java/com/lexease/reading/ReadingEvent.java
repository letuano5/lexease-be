package com.lexease.reading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reading_events")
public class ReadingEvent {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ReadingSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ReadingEventType eventType;

    private String word;

    @Column(name = "word_index")
    private Integer wordIndex;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReadingEvent() {
    }

    public ReadingEvent(
            UUID id,
            ReadingSession session,
            ReadingEventType eventType,
            String word,
            Integer wordIndex,
            Long timestampMs,
            Map<String, Object> metadata,
            Instant createdAt
    ) {
        this.id = id;
        this.session = session;
        this.eventType = eventType;
        this.word = word;
        this.wordIndex = wordIndex;
        this.timestampMs = timestampMs;
        this.metadata = metadata == null ? Map.of() : metadata;
        this.createdAt = createdAt;
    }
}
