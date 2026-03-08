package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableCurrencyExtendedInfoResponse.class)
@JsonDeserialize(as = ImmutableCurrencyExtendedInfoResponse.class)
public interface CurrencyExtendedInfoResponse {
        @Nullable
        String code();
        @Nullable
        String symbol();
        @Nullable
        String name();
        @Nullable
        String market_cap();
        @Nullable
        String circulating_supply();
        @Nullable
        String total_supply();
        @Nullable
        String max_supply();
        @Nullable
        String website();
        @Nullable
        String whitepaper();
        @Nullable
        String issue_date();
        @Nullable
        String issue_price();
        @Nullable
        Object explorer();
        @Nullable
        String roi();
        @Nullable
        String introduce_text();
}
