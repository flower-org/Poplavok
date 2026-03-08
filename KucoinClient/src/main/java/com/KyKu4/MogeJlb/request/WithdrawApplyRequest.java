/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by zicong.lu on 2018/12/14.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Value.Immutable
@JsonSerialize(as = ImmutableWithdrawApplyRequest.class)
@JsonDeserialize(as = ImmutableWithdrawApplyRequest.class)
public interface WithdrawApplyRequest {

    /**
     * Currency
     */
    String currency();

    /**
     * Withdrawal address
     */
    String address();

    /**
     * [Optional] Address remark. If there’s no remark, it is empty. When you withdraw from other platforms to the KuCoin, you need to fill in memo(tag). If you do not fill memo (tag), your deposit may not be available, please be cautious.
     */
    @Nullable
    String memo();

    /**
     * Withdrawal amount, a multiple and positive number of the amount precision (fees excluded)
     */
    BigDecimal amount();

    /**
     * [optional] Internal withdrawal or not. Default setup: false
     */
    @Value.Default
    default Boolean isInner() {
        return false;
    }

    /**
     * [optional] Remark
     */
    @Nullable
    String remark();

    /**
     * [Optional] The chain of currency. For a currency with multiple chains, it is recommended to specify chain parameter instead of using the default chain; you can query the chain through the response of the GET /api/v2/currencies/{currency} interface.
     */
    String chain();

    /**
     * Withdrawal fee deduction type: INTERNAL or EXTERNAL or not specified
     *
     * 1. INTERNAL- deduct the transaction fees from your withdrawal amount
     * 2. EXTERNAL- deduct the transaction fees from your main account
     * 3. If you don't specify the feeDeductType parameter, when the balance in your main account is sufficient to support the withdrawal, the system will initially deduct the transaction fees from your main account. But if the balance in your main account is not sufficient to support the withdrawal, the system will deduct the fees from your withdrawal amount. For example: Suppose you are going to withdraw 1 BTC from the KuCoin platform (transaction fee: 0.0001BTC), if the balance in your main account is insufficient, the system will deduct the transaction fees from your withdrawal amount. In this case, you will be receiving 0.9999BTC.
     */
    //TODO: I'm not sure why I commented this out
    // The field is not mandatory, so everything works fine without it
    // It also doesn't really matter for inner transfer, because no fees.
    // Did the request fail with the field present? That's what I don't really remember.
/*    @Nullable
    @Value.Default
    default String feeDeductType() { return "EXTERNAL"; }*/
}
