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
    @Nullable
    private Long id;

    @Column
    @Nullable
    String symbol;

    @Column(columnDefinition = "TEXT")
    @Nullable
    private String currencyExtendedInfoJson;

    public CurrencyExtendedInfo() {
    }

    public CurrencyExtendedInfo(String symbol) {
        this.symbol = symbol;
    }

    // Accessor methods matching the interface requested

    @Nullable
    public Long currencyExtendedInfoId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    @Nullable
    public String symbol() {
        return symbol;
    }

    public void setSymbol(@Nullable String symbol) {
        this.symbol = symbol;
    }

    @Nullable
    public String getCurrencyExtendedInfoJson() {
        return currencyExtendedInfoJson;
    }

    public void setCurrencyExtendedInfoJson(@Nullable String currencyExtendedInfoJson) {
        this.currencyExtendedInfoJson = currencyExtendedInfoJson;
    }
}

