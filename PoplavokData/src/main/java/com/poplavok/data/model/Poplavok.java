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
@Table(name = "poplavoks")
public class Poplavok {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    @Nullable
    private MarketTicker marketTicker;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_strategy", nullable = false, length = 30)
    @Nullable
    private LevelStrategy levelStrategy;

    @Column(name = "strategy_parameters", length = 500)
    @Nullable
    private String strategyParameters;

    @Column
    @Nullable
    private String name;

    @Column(length = 2000)
    @Nullable
    private String details;

    @Column(name = "is_active", nullable = false)
    @Nullable
    private boolean isActive;

    @Column(name = "entry_price", precision = 20, scale = 8)
    @Nullable
    private BigDecimal entryPrice;

    @Column(name = "creation_date", nullable = false)
    @Nullable
    private LocalDateTime creationDate;

    @Column(name = "close_date")
    @Nullable
    private LocalDateTime closeDate;

    @OneToMany(mappedBy = "poplavok", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Level> levels = new ArrayList<>();

    @OneToMany(mappedBy = "poplavok", cascade = CascadeType.ALL)
    private List<Loan> loans = new ArrayList<>();

    @OneToMany(mappedBy = "poplavok", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationHistory> operationHistory = new ArrayList<>();

    protected Poplavok() {
    }

    public Poplavok(MarketTicker marketTicker, LevelStrategy levelStrategy, String strategyParameters,
                    BigDecimal entryPrice, LocalDateTime creationDate) {
        this.marketTicker = marketTicker;
        this.levelStrategy = levelStrategy;
        this.strategyParameters = strategyParameters;
        this.entryPrice = entryPrice;
        this.creationDate = creationDate;
        this.isActive = true;
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

    public LevelStrategy getLevelStrategy() {
        return checkNotNull(levelStrategy);
    }

    public void setLevelStrategy(LevelStrategy levelStrategy) {
        this.levelStrategy = levelStrategy;
    }

    public String getStrategyParameters() {
        return checkNotNull(strategyParameters);
    }

    public void setStrategyParameters(String strategyParameters) {
        this.strategyParameters = strategyParameters;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getDetails() {
        return details;
    }

    public void setDetails(@Nullable String details) {
        this.details = details;
    }

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(length = 10)
    @Nullable
    private Direction direction;

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public BigDecimal getEntryPrice() {
        return checkNotNull(entryPrice);
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
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

    public List<Level> getLevels() {
        return levels;
    }

    public void addLevel(Level level) {
        levels.add(level);
        level.setPoplavok(this);
    }

    @Nullable
    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void removeLevel(Level level) {
        levels.remove(level);
        level.setPoplavok(null);
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public void addLoan(Loan loan) {
        loans.add(loan);
        loan.setPoplavok(this);
    }

    public List<OperationHistory> getOperationHistory() {
        return operationHistory;
    }

    public void addOperationHistory(OperationHistory operation) {
        operationHistory.add(operation);
        operation.setPoplavok(this);
    }
}

