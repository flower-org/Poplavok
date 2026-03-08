/*
 * Copyright (c) 2019 Mek Global Limited
 */
package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableMarkPriceResponse.class)
@JsonDeserialize(as = ImmutableMarkPriceResponse.class)
public interface MarkPriceResponse //implements Serializable
{

    //static final long serialVersionUID = -5271075504620444867L;

    @Nullable
    String symbol();

    @Nullable
    Long granularity();

    @Nullable
    Long timePoint();

    @Nullable
    BigDecimal value();

}
