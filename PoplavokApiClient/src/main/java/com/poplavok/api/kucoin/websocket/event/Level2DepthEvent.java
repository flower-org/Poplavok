package com.poplavok.api.kucoin.websocket.event;

import com.poplavok.api.kucoin.model.response.Level2DepthResponse;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLevel2DepthEvent.class)
@JsonDeserialize(as = ImmutableLevel2DepthEvent.class)
public interface Level2DepthEvent extends Level2DepthResponse {
}
