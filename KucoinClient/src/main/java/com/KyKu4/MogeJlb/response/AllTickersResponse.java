package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by chenshiwei on 2019/2/22.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAllTickersResponse.class)
@JsonDeserialize(as = ImmutableAllTickersResponse.class)
public interface AllTickersResponse {

    @Nullable
    Long time();

    @Nullable
    List<MarketTickerResponse> ticker();

}
