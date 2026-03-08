package com.KyKu4.MogeJlb.websocket.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableLevel2Event.class)
@JsonDeserialize(as = ImmutableLevel2Event.class)
public interface Level2Event {

    @Nullable
    Object[][] asks();

    @Nullable
    Object[][] bids();

    @Nullable
    Long timestamp();
}
