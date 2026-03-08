/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by zicong.lu on 2018/12/21.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableDepositAddressCreateRequest.class)
@JsonDeserialize(as = ImmutableDepositAddressCreateRequest.class)
public interface DepositAddressCreateRequest {

    @Nullable
    String currency();

    @Nullable
    String chain();

}
