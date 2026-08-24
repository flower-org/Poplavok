package com.poplavok.kucoin;

import com.poplavok.api.kucoin.model.response.ApiCurrencyDetailChainPropertyResponse;
import com.poplavok.api.kucoin.model.response.CurrencyExtendedInfoResponse;
import com.poplavok.api.kucoin.model.response.CurrencyResponse;
import com.poplavok.api.kucoin.model.response.ImmutableCurrencyExtendedInfoResponse;
import com.poplavok.api.kucoin.model.response.MarketTickerResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.CurrencyChain;
import com.poplavok.data.model.CurrencyExtendedInfo;
import com.poplavok.data.model.MarketTicker;

import javax.annotation.Nullable;
import java.math.BigDecimal;

public class EntityConverter {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.registerModules(new GuavaModule());
    }

    public static Currency fromResponse(CurrencyResponse currencyResponse) {
        return of(currencyResponse.currency(), Strings.nullToEmpty(currencyResponse.fullName()), Strings.nullToEmpty(currencyResponse.name()),
                currencyResponse.precision(), currencyResponse.withdrawalMinSize() != null ? currencyResponse.withdrawalMinSize() : java.math.BigDecimal.ZERO, currencyResponse.withdrawalMinFee() != null ? currencyResponse.withdrawalMinFee() : java.math.BigDecimal.ZERO,
                Boolean.TRUE.equals(currencyResponse.isWithdrawEnabled()), Boolean.TRUE.equals(currencyResponse.isDepositEnabled()), Boolean.TRUE.equals(currencyResponse.isMarginEnabled()),
                Boolean.TRUE.equals(currencyResponse.isDebitEnabled()));
    }

    private static Currency of(String currency, String fullName, String name, Integer precision,
                               BigDecimal withdrawalMinSize, BigDecimal withdrawalMinFee,
                               Boolean isWithdrawEnabled, Boolean isDepositEnabled,
                               Boolean isMarginEnabled, Boolean isDebitEnabled) {
        Currency c = new Currency(currency);
        if (currency != null) {
            c.setCurrency(currency);
        }
        c.setFullName(fullName);
        if (name != null) {
            c.setName(name);
        }
        c.setPrecision(precision);
        c.setWithdrawalMinSize(withdrawalMinSize != null ? withdrawalMinSize.toString() : null);
        c.setWithdrawalMinFee(withdrawalMinFee != null ? withdrawalMinFee.toString() : null);
        c.setIsWithdrawEnabled(isWithdrawEnabled);
        c.setIsDepositEnabled(isDepositEnabled);
        c.setIsMarginEnabled(isMarginEnabled);
        c.setIsDebitEnabled(isDebitEnabled);
        return c;
    }

    // --------------------------------------------

    public static MarketTicker fromResponse(String baseCurrency, String quoteCurrency, MarketTickerResponse marketTickerResponse) {
        return fromResponse(null, baseCurrency, quoteCurrency, marketTickerResponse);
    }

    public static MarketTicker fromResponse(@Nullable Long marketTickerId, String baseCurrency, String quoteCurrency, MarketTickerResponse marketTickerResponse) {
        return of(marketTickerId, baseCurrency, quoteCurrency, Strings.nullToEmpty(marketTickerResponse.symbol()),
                Strings.nullToEmpty(marketTickerResponse.symbolName()), marketTickerResponse.takerFeeRate() != null ? marketTickerResponse.takerFeeRate() : java.math.BigDecimal.ZERO, marketTickerResponse.makerFeeRate() != null ? marketTickerResponse.makerFeeRate() : java.math.BigDecimal.ZERO,
                marketTickerResponse.takerCoefficient() != null ? marketTickerResponse.takerCoefficient() : java.math.BigDecimal.ZERO, marketTickerResponse.makerCoefficient() != null ? marketTickerResponse.makerCoefficient() : java.math.BigDecimal.ZERO);
    }

    private static MarketTicker of(@Nullable Long marketTickerId, String baseCurrency, String quoteCurrency, String symbol,
                                   String symbolName, BigDecimal takerFeeRate, BigDecimal makerFeeRate,
                                   BigDecimal takerCoefficient, BigDecimal makerCoefficient) {
        MarketTicker ticker = new MarketTicker();
        ticker.setId(marketTickerId);
        Currency base = new Currency();
        base.setCurrency(baseCurrency);
        ticker.setBase(base);
        Currency quote = new Currency();
        quote.setCurrency(quoteCurrency);
        ticker.setQuote(quote);
        ticker.setSymbol(symbol);
        ticker.setSymbolName(symbolName);
        ticker.setTakerFeeRate(takerFeeRate != null ? takerFeeRate.toString() : null);
        ticker.setMakerFeeRate(makerFeeRate != null ? makerFeeRate.toString() : null);
        ticker.setTakerCoefficient(takerCoefficient != null ? takerCoefficient.toString() : null);
        ticker.setMakerCoefficient(makerCoefficient != null ? makerCoefficient.toString() : null);
        return ticker;
    }

    // --------------------------------------------

    public static CurrencyChain fromResponse(String currency, String chain, ApiCurrencyDetailChainPropertyResponse marketTickerResponse) {
        return fromResponse(null, currency, chain, marketTickerResponse);
    }

    public static CurrencyChain fromResponse(@Nullable Long currencyChainId, String currency, @Nullable String chain, ApiCurrencyDetailChainPropertyResponse marketTickerResponse) {
        return of(currencyChainId, currency, chain,
                marketTickerResponse.minWithdrawSize() != null ? marketTickerResponse.minWithdrawSize().toString() : null,
                marketTickerResponse.minWithdrawFee() != null ? marketTickerResponse.minWithdrawFee().toString() : null,
                marketTickerResponse.isWithdrawEnabled(), marketTickerResponse.isDepositEnabled(), marketTickerResponse.confirms(), marketTickerResponse.contractAddress());
    }

    private static CurrencyChain of(@Nullable Long id, String currency, @Nullable String chain_, @Nullable String withdrawalMinSize, @Nullable String withdrawalMinFee,
                                    @Nullable Boolean isWithdrawEnabled, @Nullable Boolean isDepositEnabled, @Nullable Integer confirms, @Nullable String contractAddress) {
        CurrencyChain chain = new CurrencyChain(currency, chain_);
        if (id != null && id != -1L) {
            chain.setId(id);
        }
        chain.setWithdrawalMinSize(withdrawalMinSize);
        chain.setWithdrawalMinFee(withdrawalMinFee);
        chain.setIsWithdrawEnabled(isWithdrawEnabled);
        chain.setIsDepositEnabled(isDepositEnabled);
        chain.setConfirms(confirms);
        chain.setContractAddress(contractAddress);
        return chain;
    }

    // --------------------------------------------

    public static CurrencyExtendedInfo fromResponse(String currency, CurrencyExtendedInfoResponse currencyExtendedInfoResponse) {
        try {
            String currencyExtendedInfoJson = OBJECT_MAPPER.writeValueAsString(currencyExtendedInfoResponse);
            return of(currency, currencyExtendedInfoJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static CurrencyExtendedInfo of(String currency, String currencyExtendedInfoJson) {
        CurrencyExtendedInfo info = new CurrencyExtendedInfo(currency);
        info.setCurrency(currency);
        info.setCurrencyExtendedInfoJson(currencyExtendedInfoJson);
        return info;
    }

    public static CurrencyExtendedInfoResponse fromCurrencyExtendedInfo(CurrencyExtendedInfo info) {
        try {
            String currencyExtendedInfoJson = info.getCurrencyExtendedInfoJson();
            return OBJECT_MAPPER.readValue(currencyExtendedInfoJson, ImmutableCurrencyExtendedInfoResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
