/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by tao.mao on 2018/11/15.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableAccountBalancesResponse.class)
@JsonDeserialize(as = ImmutableAccountBalancesResponse.class)
public interface AccountBalancesResponse {

    /** The ID of the account */
    @Nullable
    String id();

    /** Currency */
    @Nullable
    String currency();

    /** Account type: main, trade, margin */
    @Nullable
    String type();

    /** Total funds in the account */
    @Nullable
    BigDecimal balance();

    /** Funds available to withdraw or trade */
    @Nullable
    BigDecimal available();

    /** Funds on hold (not available for use) */
    @Nullable
    BigDecimal holds();

}
