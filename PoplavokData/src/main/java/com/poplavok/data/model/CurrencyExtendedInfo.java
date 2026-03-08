package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.annotation.Nullable;

@Entity
@Table(name = "currency_extended_info")
public class CurrencyExtendedInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @Column
    @Nullable
    private String code;

    @Column(nullable = false)
    @Nullable
    private String symbol;

    @Column
    @Nullable
    private String name;

    @Column
    @Nullable
    private String marketCap;

    @Column
    @Nullable
    private String circulatingSupply;

    @Column
    @Nullable
    private String totalSupply;

    @Column
    @Nullable
    private String maxSupply;

    @Column(length = 2048)
    @Nullable
    private String website;

    @Column(length = 2048)
    @Nullable
    private String whitepaper;

    @Column
    @Nullable
    private String issueDate;

    @Column
    @Nullable
    private String issuePrice;

    @Column(length = 2048)
    @Nullable
    private String explorer;

    @Column
    @Nullable
    private String roi;

    @Column(columnDefinition = "TEXT")
    @Nullable
    private String introduceText;

    public CurrencyExtendedInfo() {
    }

    // Accessor methods matching the interface requested

    @Nullable
    public Long currencyExtendedInfoId() {
        return id;
    }

    @Nullable
    public String code() {
        return code;
    }

    @Nullable
    public String symbol() {
        return symbol;
    }

    @Nullable
    public String name() {
        return name;
    }

    @Nullable
    public String marketCap() {
        return marketCap;
    }

    @Nullable
    public String circulatingSupply() {
        return circulatingSupply;
    }

    @Nullable
    public String totalSupply() {
        return totalSupply;
    }

    @Nullable
    public String maxSupply() {
        return maxSupply;
    }

    @Nullable
    public String website() {
        return website;
    }

    @Nullable
    public String whitepaper() {
        return whitepaper;
    }

    @Nullable
    public String issueDate() {
        return issueDate;
    }

    @Nullable
    public String issuePrice() {
        return issuePrice;
    }

    @Nullable
    public String explorer() {
        return explorer;
    }

    @Nullable
    public String roi() {
        return roi;
    }

    @Nullable
    public String introduceText() {
        return introduceText;
    }


    // Standard Getters and Setters for Hibernate / other usage

    @Nullable
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Nullable
    public String getCode() {
        return code;
    }

    public void setCode(@Nullable String code) {
        this.code = code;
    }

    @Nullable
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(@Nullable String marketCap) {
        this.marketCap = marketCap;
    }

    @Nullable
    public String getCirculatingSupply() {
        return circulatingSupply;
    }

    public void setCirculatingSupply(@Nullable String circulatingSupply) {
        this.circulatingSupply = circulatingSupply;
    }

    @Nullable
    public String getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(@Nullable String totalSupply) {
        this.totalSupply = totalSupply;
    }

    @Nullable
    public String getMaxSupply() {
        return maxSupply;
    }

    public void setMaxSupply(@Nullable String maxSupply) {
        this.maxSupply = maxSupply;
    }

    @Nullable
    public String getWebsite() {
        return website;
    }

    public void setWebsite(@Nullable String website) {
        this.website = website;
    }

    @Nullable
    public String getWhitepaper() {
        return whitepaper;
    }

    public void setWhitepaper(@Nullable String whitepaper) {
        this.whitepaper = whitepaper;
    }

    @Nullable
    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(@Nullable String issueDate) {
        this.issueDate = issueDate;
    }

    @Nullable
    public String getIssuePrice() {
        return issuePrice;
    }

    public void setIssuePrice(@Nullable String issuePrice) {
        this.issuePrice = issuePrice;
    }

    @Nullable
    public String getExplorer() {
        return explorer;
    }

    public void setExplorer(@Nullable String explorer) {
        this.explorer = explorer;
    }

    @Nullable
    public String getRoi() {
        return roi;
    }

    public void setRoi(@Nullable String roi) {
        this.roi = roi;
    }

    @Nullable
    public String getIntroduceText() {
        return introduceText;
    }

    public void setIntroduceText(@Nullable String introduceText) {
        this.introduceText = introduceText;
    }
}

