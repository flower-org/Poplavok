package com.KyKu4.MogeJlb.websocket.event;

import com.KyKu4.MogeJlb.response.SnapshotResponse;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by Reeta on 2019-05-21
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSnapshotEvent.class)
@JsonDeserialize(as = ImmutableSnapshotEvent.class)
public interface SnapshotEvent {

    @Nullable
    String sequence();

    @Nullable
    SnapshotResponse data();

}
