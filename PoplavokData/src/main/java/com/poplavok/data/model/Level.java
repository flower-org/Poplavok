package com.poplavok.data.model;

import jakarta.persistence.CascadeType;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "levels")
public class Level {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @Column(name = "level_num", nullable = false)
    @Nullable
    private int levelNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poplavok_id", nullable = false)
    @Nullable
    private Poplavok poplavok;

    @Column(name = "amount_base", nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal amountBase;

    @Column(name = "amount_quote", nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal amountQuote;

    @Column(name = "is_active", nullable = false)
    @Nullable
    private boolean isActive;

    @Column(name = "level_price", nullable = false, precision = 20, scale = 8)
    @Nullable
    private BigDecimal levelPrice;

    @Column(name = "creation_date", nullable = false)
    @Nullable
    private LocalDateTime creationDate;

    @Column(name = "close_date")
    @Nullable
    private LocalDateTime closeDate;

    @OneToMany(mappedBy = "level", cascade = CascadeType.ALL)
    private List<Loan> loans = new ArrayList<>();

    @OneToMany(mappedBy = "level", cascade = CascadeType.ALL)
    private List<LevelTrade> levelTrades = new ArrayList<>();

    public Level() {
    }

    public Level(int levelNum, Poplavok poplavok, BigDecimal amountBase, BigDecimal amountQuote,
                 BigDecimal levelPrice, LocalDateTime creationDate) {
        this.levelNum = levelNum;
        this.poplavok = poplavok;
        this.amountBase = amountBase;
        this.amountQuote = amountQuote;
        this.levelPrice = levelPrice;
        this.creationDate = creationDate;
        this.isActive = true;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public int getLevelNum() {
        return levelNum;
    }

    public void setLevelNum(int levelNum) {
        this.levelNum = levelNum;
    }

    public Poplavok getPoplavok() {
        return checkNotNull(poplavok);
    }

    void setPoplavok(@Nullable Poplavok poplavok) {
        this.poplavok = poplavok;
    }

    public BigDecimal getAmountBase() {
        return checkNotNull(amountBase);
    }

    public void setAmountBase(BigDecimal amountBase) {
        this.amountBase = amountBase;
    }

    public BigDecimal getAmountQuote() {
        return checkNotNull(amountQuote);
    }

    public void setAmountQuote(BigDecimal amountQuote) {
        this.amountQuote = amountQuote;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public BigDecimal getLevelPrice() {
        return checkNotNull(levelPrice);
    }

    public void setLevelPrice(BigDecimal levelPrice) {
        this.levelPrice = levelPrice;
    }

    public LocalDateTime getCreationDate() {
        return checkNotNull(creationDate);
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getCloseDate() {
        return checkNotNull(closeDate);
    }

    public void setCloseDate(LocalDateTime closeDate) {
        this.closeDate = closeDate;
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public List<LevelTrade> getLevelTrades() {
        return levelTrades;
    }

    public void addLevelTrade(LevelTrade levelTrade) {
        levelTrades.add(levelTrade);
        levelTrade.setLevel(this);
    }

    public void addLoan(Loan loan) {
        loans.add(loan);
        loan.setDestinationLevel(this);
    }
}
