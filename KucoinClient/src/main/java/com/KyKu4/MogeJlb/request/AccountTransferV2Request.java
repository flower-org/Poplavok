package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by Reeta on 2019/9/26
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAccountTransferV2Request.class)
@JsonDeserialize(as = ImmutableAccountTransferV2Request.class)
public interface AccountTransferV2Request {
    /** ClientOid, the unique identifier created by the client, use of UUID */
    String clientOid();

    /** Currency */
    String currency();

    /** Payment Account Type: main, trade, margin, or isolated */
    String from();

    /** Receiving Account Type: main, trade, margin, isolated, or contract */
    String to();

    /** Transfer amount, the precision being a positive integer multiple of the Currency Precision */
    BigDecimal amount();

    /** Trading pair, required when the payment account type is isolated, e.g.: BTC-USDT */
    @Nullable
    String fromTag();

    /** Trading pair, required when the receiving account type is isolated, e.g.: BTC-USDT */
    @Nullable
    String toTag();
}