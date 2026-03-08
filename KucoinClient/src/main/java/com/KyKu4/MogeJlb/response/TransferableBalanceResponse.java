package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by Reeta on 2019/9/29
 */
@Value.Immutable
@JsonSerialize(as = ImmutableTransferableBalanceResponse.class)
@JsonDeserialize(as = ImmutableTransferableBalanceResponse.class)
public interface TransferableBalanceResponse {

    @Nullable
    String currency();

    @Nullable
    BigDecimal balance();

    @Nullable
    BigDecimal available();

    @Nullable
    BigDecimal holds();

    @Nullable
    BigDecimal transferable();

}
