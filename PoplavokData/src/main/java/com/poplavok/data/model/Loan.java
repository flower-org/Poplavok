package com.poplavok.data.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "loans")
@PrimaryKeyJoinColumn(name = "transaction_id")
public class Loan extends Transaction {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    @Nullable
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poplavok_id")
    @Nullable
    private Poplavok poplavok;

    @Column(name = "is_active", nullable = false)
    @Nullable
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    @Nullable
    private LoanType loanType;

    @Column(name = "interest_rate", precision = 10, scale = 6)
    @Nullable
    private BigDecimal interestRate;

    @Column(length = 500)
    @Nullable
    private String notes;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Repayment> repayments = new ArrayList<>();

    protected Loan() {
    }

    public Loan(Currency currency, BigDecimal amount, Poplavok poplavok, Level level,
                LocalDateTime date, LoanType loanType) {
        super(null, null, null, level, amount, date);
        this.currency = currency;
        this.poplavok = poplavok;
        this.loanType = loanType;
        this.isActive = true;
    }

    public Currency getCurrency() {
        return checkNotNull(currency);
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Poplavok getPoplavok() {
        return checkNotNull(poplavok);
    }

    void setPoplavok(@Nullable Poplavok poplavok) {
        this.poplavok = poplavok;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LoanType getLoanType() {
        return checkNotNull(loanType);
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getInterestRate() {
        return checkNotNull(interestRate);
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public @Nullable String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Repayment> getRepayments() {
        return checkNotNull(repayments);
    }

    public void addRepayment(Repayment repayment) {
        repayments.add(repayment);
        repayment.setLoan(this);
    }

    public void removeRepayment(Repayment repayment) {
        repayments.remove(repayment);
        repayment.setLoan(null);
    }
}
