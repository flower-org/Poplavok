/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by tao.mao on 2018/9/3.
 * 用户账户内部转账
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAccountTransferRequest.class)
@JsonDeserialize(as = ImmutableAccountTransferRequest.class)
public interface AccountTransferRequest //implements Serializable
{

    //private static final long serialVersionUID = 9068593468335839904L;

    @Nullable
    String clientOid();

    @Nullable
    String payAccountId();

    @Nullable
    BigDecimal amount();

    @Nullable
    String recAccountId();

}
