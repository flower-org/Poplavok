package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by Reeta on 2019-05-20
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableSubUserInfoResponse.class)
@JsonDeserialize(as = ImmutableSubUserInfoResponse.class)
public interface SubUserInfoResponse {

    @Nullable
    String userId();

    @Nullable
    String subName();

    @Nullable
    String remarks();
}
