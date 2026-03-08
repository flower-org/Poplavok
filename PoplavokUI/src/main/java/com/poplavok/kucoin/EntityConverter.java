package com.poplavok.kucoin;

import com.KyKu4.MogeJlb.response.CurrencyResponse;
import com.KyKu4.MogeJlb.response.MarketTickerResponse;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.MarketTicker;

import javax.annotation.Nullable;
import java.math.BigDecimal;

public class EntityConverter {

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
}
