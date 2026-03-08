/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.websocket.event;

import com.KyKu4.MogeJlb.model.OrderBook;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by chenshiwei on 2019/1/11.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableLevel2ChangeEvent.class)
@JsonDeserialize(as = ImmutableLevel2ChangeEvent.class)
public interface Level2ChangeEvent {

    @Nullable
    Long sequenceStart();

    @Nullable
    Long sequenceEnd();

    @Nullable
    String symbol();

    @Nullable
    OrderBook changes();

}
