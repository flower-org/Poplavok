package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by Reeta on 2019-05-22
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSnapshotResponse.class)
@JsonDeserialize(as = ImmutableSnapshotResponse.class)
public interface SnapshotResponse {

    @Nullable
    String symbol();

    @Nullable
    String symbolCode();

    @Nullable
    BigDecimal buy();

    @Nullable
    BigDecimal sell();

    @Nullable
    BigDecimal low();

    @Nullable
    BigDecimal high();

    @Nullable
    BigDecimal open();

    @Nullable
    BigDecimal lastTradedPrice();

    @Nullable
    BigDecimal changeRate();

    @Nullable
    BigDecimal changePrice();

    @Nullable
    BigDecimal vol();

    @Nullable
    BigDecimal volValue();

    @Nullable
    BigDecimal close();

    @Nullable
    String baseCurrency();

    @Nullable
    String quoteCurrency();

    @Nullable
    Boolean trading();

    @Nullable
    Integer sort();

    @Nullable
    Integer board();

    @Nullable
    Integer mark();

    @Nullable
    String market();

    @Nullable
    Long datetime();

}
