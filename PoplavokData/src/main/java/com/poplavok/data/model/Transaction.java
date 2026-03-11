package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "transactions")
@Inheritance(strategy = InheritanceType.JOINED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    @Nullable
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_level_id")
    @Nullable
    private Level sourceLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    @Nullable
    private Account destinationAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_level_id")
    @Nullable
    private Level destinationLevel;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal amount;

    @Column(nullable = false)
    @Nullable
    private LocalDateTime date;

    public Transaction() {
    }

    public Transaction(@Nullable Account sourceAccount, @Nullable Level sourceLevel,
                       @Nullable Account destinationAccount, @Nullable Level destinationLevel,
                       BigDecimal amount, LocalDateTime date) {
        this.sourceAccount = sourceAccount;
        this.sourceLevel = sourceLevel;
        this.destinationAccount = destinationAccount;
        this.destinationLevel = destinationLevel;
        this.amount = amount;
        this.date = date;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    @Nullable
    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(@Nullable Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    @Nullable
    public Level getSourceLevel() {
        return sourceLevel;
    }

    public void setSourceLevel(@Nullable Level sourceLevel) {
        this.sourceLevel = sourceLevel;
    }

    @Nullable
    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(@Nullable Account destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    @Nullable
    public Level getDestinationLevel() {
        return destinationLevel;
    }

    public void setDestinationLevel(@Nullable Level destinationLevel) {
        this.destinationLevel = destinationLevel;
    }

    public BigDecimal getAmount() {
        return checkNotNull(amount);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDate() {
        return checkNotNull(date);
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
