package com.poplavok.data.model;

import javax.annotation.Nullable;

public class CurrencyChainEx extends CurrencyChain {
    @Nullable
    private String currency;

    @Nullable
    private String chain;

    public CurrencyChainEx() {
        super();
    }

    public CurrencyChainEx(@Nullable Long currencyId, @Nullable Long chainId) {
        super(currencyId, chainId);
    }

    // Accessors matching interface
    @Nullable
    public String currency() {
        return currency;
    }

    @Nullable
    public String chain() {
        return chain;
    }

    // Setters
    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public void setChain(@Nullable String chain) {
        this.chain = chain;
    }

    @Nullable
    public String getCurrency() {
        return currency;
    }

    @Nullable
    public String getChain() {
        return chain;
    }
}
