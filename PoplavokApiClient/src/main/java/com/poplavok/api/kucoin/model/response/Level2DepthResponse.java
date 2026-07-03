package com.poplavok.api.kucoin.model.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableLevel2DepthResponse.class)
@JsonDeserialize(as = ImmutableLevel2DepthResponse.class)
public interface Level2DepthResponse {

    @Nullable
    List<List<String>> asks();

    @Nullable
    List<List<String>> bids();

    @Nullable
    Long timestamp();
}
