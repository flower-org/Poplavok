/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.websocket.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by chenshiwei on 2019/1/10.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableKucoinEvent.class)
@JsonDeserialize(as = ImmutableKucoinEvent.class)
public interface KucoinEvent<T> {
    @Nullable
    String userId();
    @Nullable
    String channelType();

    @Nullable
    String id();

    String type();

    @Nullable
    String topic();

    @Nullable
    Boolean privateChannel();

    @Nullable
    Boolean response();

    @Nullable
    T data();

    @Nullable
    String subject();

}
