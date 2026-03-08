package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableLevel3Response.class)
@JsonDeserialize(as = ImmutableLevel3Response.class)
public interface Level3Response {

    @Nullable
    Object[][] bids();

    @Nullable
    Object[][] asks();

    @Nullable
    Long sequence();

    @Nullable
    Long time();
}
