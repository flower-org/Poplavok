package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "tickers")
public class MarketTicker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id", nullable = false)
    @Nullable
    private Currency base;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_currency_id", nullable = false)
    @Nullable
    private Currency quote;

    @Column(nullable = false)
    @Nullable
    private String symbol;

    @Column
    @Nullable
    private String symbolName;

    @Column
    @Nullable
    private String takerFeeRate;

    @Column
    @Nullable
    private String makerFeeRate;

    @Column
    @Nullable
    private String takerCoefficient;

    @Column
    @Nullable
    private String makerCoefficient;

    @OneToMany(mappedBy = "marketTicker")
    private List<Rate> rates = new ArrayList<>();

    @OneToMany(mappedBy = "marketTicker")
    private List<Poplavok> poplavoks = new ArrayList<>();

    public MarketTicker() {
    }

    public MarketTicker(Currency base, Currency quote, String symbol) {
        this.base = base;
        this.quote = quote;
        this.symbol = symbol;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public Currency getBase() {
        return checkNotNull(base);
    }

    public void setBase(Currency base) {
        this.base = base;
        }

    public Currency getQuote() {
        return checkNotNull(quote);
    }

    public void setQuote(Currency quote) {
        this.quote = quote;
    }

    public String getSymbol() {
        return checkNotNull(symbol);
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Nullable
    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(@Nullable String symbolName) {
        this.symbolName = symbolName;
    }

    @Nullable
    public String getTakerFeeRate() {
        return takerFeeRate;
    }

    public void setTakerFeeRate(@Nullable String takerFeeRate) {
        this.takerFeeRate = takerFeeRate;
    }

    @Nullable
    public String getMakerFeeRate() {
        return makerFeeRate;
    }

    public void setMakerFeeRate(@Nullable String makerFeeRate) {
        this.makerFeeRate = makerFeeRate;
    }

    @Nullable
    public String getTakerCoefficient() {
        return takerCoefficient;
    }

    public void setTakerCoefficient(@Nullable String takerCoefficient) {
        this.takerCoefficient = takerCoefficient;
    }

    @Nullable
    public String getMakerCoefficient() {
        return makerCoefficient;
    }

    public void setMakerCoefficient(@Nullable String makerCoefficient) {
        this.makerCoefficient = makerCoefficient;
    }

    public List<Rate> getRates() {
        return rates;
    }

    public List<Poplavok> getPoplavoks() {
        return poplavoks;
    }
}

