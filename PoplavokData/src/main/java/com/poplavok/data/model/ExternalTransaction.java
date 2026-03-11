package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_transactions")
@PrimaryKeyJoinColumn(name = "transaction_id")
public class ExternalTransaction extends Transaction {

    @Column(length = 500)
    @Nullable
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_type", length = 30)
    @Nullable
    private ExternalTransactionType type;

    public ExternalTransaction() {
    }

    public ExternalTransaction(@Nullable Account account, BigDecimal amount, LocalDateTime date,
                               String details, ExternalTransactionType type) {
        // External transactions usually affect accounts directly
        super(account, null, null, null, amount, date);
        this.details = details;
        this.type = type;
    }

    @Nullable
    public String getDetails() {
        return details;
    }

    public void setDetails(@Nullable String details) {
        this.details = details;
    }

    @Nullable
    public ExternalTransactionType getType() {
        return type;
    }

    public void setType(@Nullable ExternalTransactionType type) {
        this.type = type;
    }
}

