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
@Table(name = "repayments")
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @Nullable
    private Loan loan;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal amount;

    @Column(name = "repayment_date", nullable = false)
    @Nullable
    private LocalDateTime date;

    protected Repayment() {
    }

    public Repayment(Loan loan, BigDecimal amount, LocalDateTime date) {
        this.loan = loan;
        this.amount = amount;
        this.date = date;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public Loan getLoan() {
        return checkNotNull(loan);
    }

    void setLoan(@Nullable Loan loan) {
        this.loan = loan;
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

