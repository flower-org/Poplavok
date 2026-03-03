package com.poplavok.data.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    @Nullable
    private Currency currency;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poplavok_id")
    @Nullable
    private Poplavok poplavok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    @Nullable
    private Level level;

    @Column(name = "loan_date", nullable = false)
    @Nullable
    private LocalDateTime date;

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

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Repayment> repayments = new ArrayList<>();

    protected Loan() {
    }

    public Loan(Currency currency, BigDecimal amount, Poplavok poplavok, Level level,
                LocalDateTime date, LoanType loanType) {
        this.currency = currency;
        this.amount = amount;
        this.poplavok = poplavok;
        this.level = level;
        this.date = date;
        this.loanType = loanType;
        this.isActive = true;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public Currency getCurrency() {
        return checkNotNull(currency);
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return checkNotNull(amount);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    void setLevel(@Nullable Level level) {
        this.level = level;
    }

    public LocalDateTime getDate() {
        return checkNotNull(date);
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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

