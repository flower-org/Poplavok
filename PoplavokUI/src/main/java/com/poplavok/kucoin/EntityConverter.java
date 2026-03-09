package com.poplavok.kucoin;

import com.KyKu4.MogeJlb.response.ApiCurrencyDetailChainPropertyResponse;
import com.KyKu4.MogeJlb.response.CurrencyExtendedInfoResponse;
import com.KyKu4.MogeJlb.response.CurrencyResponse;
import com.KyKu4.MogeJlb.response.ImmutableCurrencyExtendedInfoResponse;
import com.KyKu4.MogeJlb.response.MarketTickerResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        return fromResponse(null, currencyResponse);
    }

    public static Currency fromResponse(@Nullable Long currencyId, CurrencyResponse currencyResponse) {
        return of(currencyId, currencyResponse.fullName(), currencyResponse.currency(), currencyResponse.name(),
                currencyResponse.precision(), currencyResponse.withdrawalMinSize(), currencyResponse.withdrawalMinFee(),
                currencyResponse.isWithdrawEnabled(), currencyResponse.isDepositEnabled(), currencyResponse.isMarginEnabled(),
                currencyResponse.isDebitEnabled());
    }

    private static Currency of(@Nullable Long id, String fullName, String currencyCode, String name, Integer precision,
                               BigDecimal withdrawalMinSize, BigDecimal withdrawalMinFee,
                               Boolean isWithdrawEnabled, Boolean isDepositEnabled,
                               Boolean isMarginEnabled, Boolean isDebitEnabled) {
        Currency c = new Currency(currencyCode);
        if (id != null) {
            c.setId(id);
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

    public static MarketTicker fromResponse(Long baseCurrencyId, Long quoteCurrencyId, MarketTickerResponse marketTickerResponse) {
        return fromResponse(null, baseCurrencyId, quoteCurrencyId, marketTickerResponse);
    }

    public static MarketTicker fromResponse(@Nullable Long marketTickerId, Long baseCurrencyId, Long quoteCurrencyId, MarketTickerResponse marketTickerResponse) {
        return of(marketTickerId, baseCurrencyId, quoteCurrencyId, marketTickerResponse.symbol(),
                marketTickerResponse.symbolName(), marketTickerResponse.takerFeeRate(), marketTickerResponse.makerFeeRate(),
                marketTickerResponse.takerCoefficient(), marketTickerResponse.makerCoefficient());
    }

    private static MarketTicker of(@Nullable Long marketTickerId, Long baseCurrencyId, Long quoteCurrencyId, String symbol,
                                   String symbolName, BigDecimal takerFeeRate, BigDecimal makerFeeRate,
                                   BigDecimal takerCoefficient, BigDecimal makerCoefficient) {
        MarketTicker ticker = new MarketTicker();
        ticker.setId(marketTickerId);
        Currency base = new Currency();
        base.setId(baseCurrencyId);
        ticker.setBase(base);
        Currency quote = new Currency();
        quote.setId(quoteCurrencyId);
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

    public static CurrencyChain fromResponse(Long currencyId, Long chainId, ApiCurrencyDetailChainPropertyResponse marketTickerResponse) {
        return fromResponse(-1L, currencyId, chainId, marketTickerResponse);
    }

    public static CurrencyChain fromResponse(@Nullable Long currencyChainId, @Nullable Long currencyId, @Nullable Long chainId, ApiCurrencyDetailChainPropertyResponse marketTickerResponse) {
        return of(currencyChainId, currencyId, chainId,
                marketTickerResponse.minWithdrawSize() != null ? marketTickerResponse.minWithdrawSize().toString() : null,
                marketTickerResponse.minWithdrawFee() != null ? marketTickerResponse.minWithdrawFee().toString() : null,
                marketTickerResponse.isWithdrawEnabled(), marketTickerResponse.isDepositEnabled(), marketTickerResponse.confirms(), marketTickerResponse.contractAddress());
    }

    private static CurrencyChain of(@Nullable Long id, @Nullable Long currencyId, @Nullable Long chainId, @Nullable String withdrawalMinSize, @Nullable String withdrawalMinFee,
                                    @Nullable Boolean isWithdrawEnabled, @Nullable Boolean isDepositEnabled, @Nullable Integer confirms, @Nullable String contractAddress) {
        CurrencyChain chain = new CurrencyChain(currencyId, chainId);
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

    public static CurrencyExtendedInfo fromResponse(long currencyId, String symbol, CurrencyExtendedInfoResponse currencyExtendedInfoResponse) {
        try {
            String currencyExtendedInfoJson = OBJECT_MAPPER.writeValueAsString(currencyExtendedInfoResponse);
            return of(currencyId, symbol, currencyExtendedInfoJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static CurrencyExtendedInfo of(Long id, String symbol, String currencyExtendedInfoJson) {
        CurrencyExtendedInfo info = new CurrencyExtendedInfo(symbol);
        if (id != null) {
            info.setId(id);
        }
        info.setSymbol(symbol);
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
