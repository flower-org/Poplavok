package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "repayments")
@PrimaryKeyJoinColumn(name = "transaction_id")
public class Repayment extends Transaction {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @Nullable
    private Loan loan;

    @Column(length = 500)
    @Nullable
    private String notes;

    protected Repayment() {
    }

    public Repayment(Loan loan, BigDecimal amount, LocalDateTime date) {
        // Diagram links Repayment to Level.
        // Assuming Repayment is related to a specific level (maybe repayment happens at a level?).
        super(null, null, null, null, amount, date);
        this.loan = loan;
    }

    public Loan getLoan() {
        return checkNotNull(loan);
    }

    void setLoan(@Nullable Loan loan) {
        this.loan = loan;
    }

    public @Nullable String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

