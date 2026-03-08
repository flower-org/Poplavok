/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by tao.mao on 2019/1/2.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAccountCreateRequest.class)
@JsonDeserialize(as = ImmutableAccountCreateRequest.class)
public interface AccountCreateRequest {

    /**
     * Currency code
     */
    @Nullable
    String currency();

    /**
     * Account type，"main" or "trade"
     */
    @Nullable
    String type();
}
