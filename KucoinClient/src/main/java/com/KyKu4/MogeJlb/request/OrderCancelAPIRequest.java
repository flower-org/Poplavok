/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * 订单操作Facade Request类
 *
 * @author 屈亮
 * @since 2018-09-17
 */
@Value.Immutable
@JsonSerialize(as = ImmutableOrderCancelAPIRequest.class)
@JsonDeserialize(as = ImmutableOrderCancelAPIRequest.class)
public interface OrderCancelAPIRequest {

    @Nullable
    String symbol();
}