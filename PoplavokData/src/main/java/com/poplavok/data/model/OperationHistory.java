package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "operation_history")
public class OperationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poplavok_id", nullable = false)
    @Nullable
    private Poplavok poplavok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    @Nullable
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    @Nullable
    private OperationType operationType;

    @Column(precision = 20, scale = 8, nullable = false)
    @Nullable
    private BigDecimal amount;

    @Column(precision = 20, scale = 8, nullable = false)
    @Nullable
    private BigDecimal price;

    @Column(nullable = false)
    @Nullable
    private LocalDateTime timestamp;

    @Column(length = 500)
    @Nullable
    private String details;

    public OperationHistory() {
    }

    public OperationHistory(Poplavok poplavok, Level level, OperationType operationType,
                            BigDecimal amount, BigDecimal price, LocalDateTime timestamp, String details) {
        this.poplavok = poplavok;
        this.level = level;
        this.operationType = operationType;
        this.amount = amount;
        this.price = price;
        this.timestamp = timestamp;
        this.details = details;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public Poplavok getPoplavok() {
        return checkNotNull(poplavok);
    }

    void setPoplavok(Poplavok poplavok) {
        this.poplavok = poplavok;
    }

    public Level getLevel() {
        return checkNotNull(level);
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public OperationType getOperationType() {
        return checkNotNull(operationType);
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return checkNotNull(amount);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPrice() {
        return checkNotNull(price);
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return checkNotNull(timestamp);
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return checkNotNull(details);
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

