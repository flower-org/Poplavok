/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.poplavok.api.kucoin.model.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableMarginAccount.class)
@JsonDeserialize(as = ImmutableMarginAccount.class)
public interface MarginAccount {

    @Nullable
    String currency();

    @Nullable
    BigDecimal totalBalance();

    @Nullable
    BigDecimal availableBalance();

    @Nullable
    BigDecimal holdBalance();

    @Nullable
    BigDecimal liability();

    @Nullable
    BigDecimal maxBorrowSize();
}
