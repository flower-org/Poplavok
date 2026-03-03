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

/**
 * External loan entity with interest rate tracking.
 * Represents loans taken from external sources (e.g., exchanges).
 */
@Entity
@Table(name = "external_loans")
public class ExternalLoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @Nullable
    private Loan loan;

    @Column(name = "external_id", length = 100)
    @Nullable
    private String externalId;

    @Column(name = "daily_interest_rate", precision = 10, scale = 8)
    @Nullable
    private BigDecimal dailyInterestRate;

    @Column(name = "accrued_interest", precision = 20, scale = 8)
    @Nullable
    private BigDecimal accruedInterest;

    @Column(name = "last_interest_update")
    @Nullable
    private LocalDateTime lastInterestUpdate;

    public ExternalLoan() {
    }

    public ExternalLoan(Loan loan, String externalId, BigDecimal dailyInterestRate) {
        this.loan = loan;
        this.externalId = externalId;
        this.dailyInterestRate = dailyInterestRate;
        this.accruedInterest = BigDecimal.ZERO;
        this.lastInterestUpdate = LocalDateTime.now();
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public Loan getLoan() {
        return checkNotNull(loan);
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public String getExternalId() {
        return checkNotNull(externalId);
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public BigDecimal getDailyInterestRate() {
        return checkNotNull(dailyInterestRate);
    }

    public void setDailyInterestRate(BigDecimal dailyInterestRate) {
        this.dailyInterestRate = dailyInterestRate;
    }

    public BigDecimal getAccruedInterest() {
        return checkNotNull(accruedInterest);
    }

    public void setAccruedInterest(BigDecimal accruedInterest) {
        this.accruedInterest = accruedInterest;
    }

    public LocalDateTime getLastInterestUpdate() {
        return checkNotNull(lastInterestUpdate);
    }

    public void setLastInterestUpdate(LocalDateTime lastInterestUpdate) {
        this.lastInterestUpdate = lastInterestUpdate;
    }
}

