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
@Table(name = "rates")
public class Rate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    @Nullable
    private MarketTicker marketTicker;

    @Column(nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal price;

    @Column(name = "ticker_price", precision = 20, scale = 8)
    @Nullable
    private BigDecimal tickerPrice;

    @Column(nullable = false)
    @Nullable
    private LocalDateTime timestamp;

    public Rate() {
    }

    public Rate(MarketTicker marketTicker, BigDecimal price, BigDecimal tickerPrice, LocalDateTime timestamp) {
        this.marketTicker = marketTicker;
        this.price = price;
        this.tickerPrice = tickerPrice;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public MarketTicker getTicker() {
        return checkNotNull(marketTicker);
    }

    public void setTicker(MarketTicker marketTicker) {
        this.marketTicker = marketTicker;
    }

    public BigDecimal getPrice() {
        return checkNotNull(price);
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTickerPrice() {
        return checkNotNull(tickerPrice);
    }

    public void setTickerPrice(BigDecimal tickerPrice) {
        this.tickerPrice = tickerPrice;
    }

    public LocalDateTime getTimestamp() {
        return checkNotNull(timestamp);
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

