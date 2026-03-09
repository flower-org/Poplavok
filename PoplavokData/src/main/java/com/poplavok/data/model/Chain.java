package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.annotation.Nullable;

@Entity
@Table(name = "chains")
public class Chain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @Column
    @Nullable
    private String chainName;

    @Column(nullable = false, unique = true)
    @Nullable
    private String chain;

    public Chain() {
    }

    public static Chain ofNew(@Nullable String chainName, @Nullable String chain) {
        Chain c = new Chain();
        c.setChainName(chainName);
        c.setChain(chain);
        return c;
    }

    // Accessors matching the interface request
    @Nullable
    public Long chainId() {
        return id;
    }

    @Nullable
    public String chainName() {
        return chainName;
    }

    @Nullable
    public String chain() {
        return chain;
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
    public String getChainName() {
        return chainName;
    }

    public void setChainName(@Nullable String chainName) {
        this.chainName = chainName;
    }

    @Nullable
    public String getChain() {
        return chain;
    }

    public void setChain(@Nullable String chain) {
        this.chain = chain;
    }
}

