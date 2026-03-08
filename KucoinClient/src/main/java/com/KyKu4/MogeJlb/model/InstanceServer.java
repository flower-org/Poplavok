/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by chenshiwei on 2019/1/15.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableInstanceServer.class)
@JsonDeserialize(as = ImmutableInstanceServer.class)
public interface InstanceServer {

    @Nullable
    Long pingInterval();

    @Nullable
    String endpoint();

    @Nullable
    String protocol();

    @Nullable
    Boolean encrypt();

    @Nullable
    Long pingTimeout();
}
