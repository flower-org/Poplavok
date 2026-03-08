package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by tao.mao on 2020/12/2.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableServiceStatusResponse.class)
@JsonDeserialize(as = ImmutableServiceStatusResponse.class)
public interface ServiceStatusResponse {

    @Nullable
    String status();

    @Nullable
    String msg();

}
