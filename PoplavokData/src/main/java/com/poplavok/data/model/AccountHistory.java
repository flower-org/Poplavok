package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "account_history")
public class AccountHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @Nullable
    private Account account;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal availableBefore;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal availableAfter;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal borrowedBefore;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal borrowedAfter;

    @Column(nullable = false)
    @Nullable
    private LocalDateTime timestamp;

    @Column(length = 100)
    @Nullable
    private String description;

    protected AccountHistory() {
    }

    public AccountHistory(Account account, BigDecimal availableBefore, BigDecimal availableAfter,
                          BigDecimal borrowedBefore, BigDecimal borrowedAfter,
                          LocalDateTime timestamp, String description) {
        this.account = account;
        this.availableBefore = availableBefore;
        this.availableAfter = availableAfter;
        this.borrowedBefore = borrowedBefore;
        this.borrowedAfter = borrowedAfter;
        this.timestamp = timestamp;
        this.description = description;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public Account getAccount() {
        return checkNotNull(account);
    }

    void setAccount(@Nullable Account account) {
        this.account = account;
    }

    public BigDecimal getAvailableBefore() {
        return checkNotNull(availableBefore);
    }

    public void setAvailableBefore(BigDecimal availableBefore) {
        this.availableBefore = availableBefore;
    }

    public BigDecimal getAvailableAfter() {
        return checkNotNull(availableAfter);
    }

    public void setAvailableAfter(BigDecimal availableAfter) {
        this.availableAfter = availableAfter;
    }

    public BigDecimal getBorrowedBefore() {
        return checkNotNull(borrowedBefore);
    }

    public void setBorrowedBefore(BigDecimal borrowedBefore) {
        this.borrowedBefore = borrowedBefore;
    }

    public BigDecimal getBorrowedAfter() {
        return checkNotNull(borrowedAfter);
    }

    public void setBorrowedAfter(BigDecimal borrowedAfter) {
        this.borrowedAfter = borrowedAfter;
    }

    public LocalDateTime getTimestamp() {
        return checkNotNull(timestamp);
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return checkNotNull(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

