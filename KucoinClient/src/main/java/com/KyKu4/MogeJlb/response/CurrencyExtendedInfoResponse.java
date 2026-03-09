package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableCurrencyExtendedInfoResponse.class)
@JsonDeserialize(as = ImmutableCurrencyExtendedInfoResponse.class)
public interface CurrencyExtendedInfoResponse {
        @Nullable
        String code();
        @Nullable
        String coinName();
        @Nullable
        String logo();
        @Nullable
        Integer rank();
        @Nullable
        BigDecimal ath();
        @Nullable
        BigDecimal atl();
        @Nullable
        BigDecimal marketCap();
        @Nullable
        BigDecimal totalMarketCap();
        @Nullable
        BigDecimal marketCapChange();
        @Nullable
        BigDecimal maxSupply();
        @Nullable
        BigDecimal fullyDilutedValuation();
        @Nullable
        BigDecimal marketShare();
        @Nullable
        BigDecimal volumeDivideMarketCap();
        @Nullable
        BigDecimal circulatingSupply();
        @Nullable
        String dateAdded();
        @Nullable
        BigDecimal addOptionalRatio();
        @Nullable
        BigDecimal circulateRatio();
        @Nullable
        BigDecimal currentPrice();
        @Nullable
        BigDecimal volume24h();
        @Nullable
        String rating();
        @Nullable
        Object tags();
        @Nullable
        List<String> website();
        @Nullable
        List<String> doc();
        @Nullable
        List<String> explorer();
        @Nullable
        Object contract();
        @Nullable
        Object auditAgency();
        @Nullable
        List<String> codeAndCommunity();
        @Nullable
        Object investor();
        @Nullable
        List<InfoRecord> currencyIntroduction();
        @Nullable
        Object priceLiveData();
        @Nullable
        List<QuestionAnswer> faqs();
        @Nullable
        Object oneWordIntroduction();
        @Nullable
        Object currencyTagList();
        @Nullable
        Object currencyReferenceRecommend();

        @Nullable
        String dataSource();
        @Nullable
        Object activities();
        @Nullable
        Object topics();
        @Nullable
        Object arriveCountdown();
        @Nullable
        Object riskWarning();
        @Nullable
        String jumpSuffix();
        @Nullable
        String siteType();
        @Nullable
        Object articleRecommendList();

        @Nullable
        List<InfoRecord> whitePaperList();
        @Nullable
        List<InfoRecord> sustainabilityReportList();
        @Nullable
        Object klineType();
}
