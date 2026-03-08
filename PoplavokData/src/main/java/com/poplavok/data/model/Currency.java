package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "currencies")
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String currency;

    @Column
    @Nullable
    private String name;

    @Column
    @Nullable
    private String fullName;

    @Column(name = "`precision`")
    @Nullable
    private Integer precision;

    @Column
    @Nullable
    private String withdrawalMinSize;

    @Column
    @Nullable
    private String withdrawalMinFee;

    @Column
    @Nullable
    private Boolean isWithdrawEnabled;

    @Column
    @Nullable
    private Boolean isDepositEnabled;

    @Column
    @Nullable
    private Boolean isMarginEnabled;

    @Column
    @Nullable
    private Boolean isDebitEnabled;

    @OneToMany(mappedBy = "currency")
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "currency")
    private List<Loan> loans = new ArrayList<>();

    public Currency() {
        this.currency = "";
    }

    public Currency(String currency) {
        this.currency = currency;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    @Nullable
    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrency() {
        return checkNotNull(currency);
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getFullName() {
        return fullName;
    }

    public void setFullName(@Nullable String fullName) {
        this.fullName = fullName;
    }

    @Nullable
    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(@Nullable Integer precision) {
        this.precision = precision;
    }

    @Nullable
    public String getWithdrawalMinSize() {
        return withdrawalMinSize;
    }

    public void setWithdrawalMinSize(@Nullable String withdrawalMinSize) {
        this.withdrawalMinSize = withdrawalMinSize;
    }

    @Nullable
    public String getWithdrawalMinFee() {
        return withdrawalMinFee;
    }

    public void setWithdrawalMinFee(@Nullable String withdrawalMinFee) {
        this.withdrawalMinFee = withdrawalMinFee;
    }

    @Nullable
    public Boolean getIsWithdrawEnabled() {
        return isWithdrawEnabled;
    }

    public void setIsWithdrawEnabled(@Nullable Boolean isWithdrawEnabled) {
        this.isWithdrawEnabled = isWithdrawEnabled;
    }

    @Nullable
    public Boolean getIsDepositEnabled() {
        return isDepositEnabled;
    }

    public void setIsDepositEnabled(@Nullable Boolean isDepositEnabled) {
        this.isDepositEnabled = isDepositEnabled;
    }

    @Nullable
    public Boolean getIsMarginEnabled() {
        return isMarginEnabled;
    }

    public void setIsMarginEnabled(@Nullable Boolean isMarginEnabled) {
        this.isMarginEnabled = isMarginEnabled;
    }

    @Nullable
    public Boolean getIsDebitEnabled() {
        return isDebitEnabled;
    }

    public void setIsDebitEnabled(@Nullable Boolean isDebitEnabled) {
        this.isDebitEnabled = isDebitEnabled;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Loan> getLoans() {
        return loans;
    }
}
