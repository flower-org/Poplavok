package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import javax.annotation.Nullable;

@Entity
@Table(name = "currency_chains")
public class CurrencyChain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @Column(nullable = false)
    @Nullable
    private Long currencyId;

    @Column(nullable = false)
    @Nullable
    private Long chainId;

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
    private Integer confirms;

    @Column
    @Nullable
    private String contractAddress;

    public CurrencyChain() {
        this.currencyId = 0L;
        this.chainId = 0L;
    }

    public CurrencyChain(@Nullable Long currencyId, @Nullable Long chainId) {
        this.currencyId = currencyId;
        this.chainId = chainId;
    }

    // Accessors matching the interface request

    @Nullable
    public Long currencyChainId() {
        return id;
    }

    @Nullable
    public Long currencyId() {
        return currencyId;
    }

    @Nullable
    public Long chainId() {
        return chainId;
    }

    @Nullable
    public String withdrawalMinSize() {
        return withdrawalMinSize;
    }

    @Nullable
    public String withdrawalMinFee() {
        return withdrawalMinFee;
    }

    @Nullable
    public Boolean isWithdrawEnabled() {
        return isWithdrawEnabled;
    }

    @Nullable
    public Boolean isDepositEnabled() {
        return isDepositEnabled;
    }

    @Nullable
    public Integer confirms() {
        return confirms;
    }

    @Nullable
    public String contractAddress() {
        return contractAddress;
    }

    // Getters and Setters

    @Nullable
    public Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    @Nullable
    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(@Nullable Long currencyId) {
        this.currencyId = currencyId;
    }

    @Nullable
    public Long getChainId() {
        return chainId;
    }

    public void setChainId(@Nullable Long chainId) {
        this.chainId = chainId;
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
    public Integer getConfirms() {
        return confirms;
    }

    public void setConfirms(@Nullable Integer confirms) {
        this.confirms = confirms;
    }

    @Nullable
    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(@Nullable String contractAddress) {
        this.contractAddress = contractAddress;
    }
}
