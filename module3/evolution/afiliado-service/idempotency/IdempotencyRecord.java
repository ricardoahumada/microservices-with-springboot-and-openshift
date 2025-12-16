package com.mutualidad.afiliado.infrastructure.idempotency;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public IdempotencyRecord(String key, String response, Duration ttl) {
        this.idempotencyKey = key;
        this.response = response;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plus(ttl);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
