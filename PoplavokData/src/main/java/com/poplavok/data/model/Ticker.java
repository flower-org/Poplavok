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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "tickers")
public class Ticker {
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

    @Column(nullable = false, precision = 10, scale = 6)
    @Nullable
    private BigDecimal commission;

    @OneToMany(mappedBy = "ticker")
    private List<Rate> rates = new ArrayList<>();

    @OneToMany(mappedBy = "ticker")
    private List<Poplavok> poplavoks = new ArrayList<>();

    public Ticker() {
    }

    public Ticker(Currency base, Currency quote, BigDecimal commission) {
        this.base = base;
        this.quote = quote;
        this.commission = commission;
    }

    public Long getId() {
        return checkNotNull(id);
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

    public BigDecimal getCommission() {
        return checkNotNull(commission);
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public List<Rate> getRates() {
        return rates;
    }

    public List<Poplavok> getPoplavoks() {
        return poplavoks;
    }
}

