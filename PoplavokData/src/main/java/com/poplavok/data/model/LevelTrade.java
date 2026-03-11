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

@Entity
@Table(name = "level_trades")
public class LevelTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    @Nullable
    private Level level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    @Nullable
    private Trade trade;

    @Column(name = "amount_base_in", precision = 20, scale = 8)
    @Nullable
    private BigDecimal amountBaseIn;

    @Column(name = "amount_base_out", precision = 20, scale = 8)
    @Nullable
    private BigDecimal amountBaseOut;

    @Column(name = "amount_quote_in", precision = 20, scale = 8)
    @Nullable
    private BigDecimal amountQuoteIn;

    @Column(name = "amount_quote_out", precision = 20, scale = 8)
    @Nullable
    private BigDecimal amountQuoteOut;

    public LevelTrade() {
    }

    public @Nullable Long getId() {
        return id;
    }

    public @Nullable Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public @Nullable Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public @Nullable BigDecimal getAmountBaseIn() {
        return amountBaseIn;
    }

    public void setAmountBaseIn(BigDecimal amountBaseIn) {
        this.amountBaseIn = amountBaseIn;
    }

    public @Nullable BigDecimal getAmountBaseOut() {
        return amountBaseOut;
    }

    public void setAmountBaseOut(BigDecimal amountBaseOut) {
        this.amountBaseOut = amountBaseOut;
    }

    public @Nullable BigDecimal getAmountQuoteIn() {
        return amountQuoteIn;
    }

    public void setAmountQuoteIn(BigDecimal amountQuoteIn) {
        this.amountQuoteIn = amountQuoteIn;
    }

    public @Nullable BigDecimal getAmountQuoteOut() {
        return amountQuoteOut;
    }

    public void setAmountQuoteOut(BigDecimal amountQuoteOut) {
        this.amountQuoteOut = amountQuoteOut;
    }
}

