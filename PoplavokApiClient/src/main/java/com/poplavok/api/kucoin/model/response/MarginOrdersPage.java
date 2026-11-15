package com.poplavok.api.kucoin.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/** Page of finished (HF) margin orders, newest first. Pass {@code lastId} back to get the next (older) page. */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableMarginOrdersPage.class)
@JsonDeserialize(as = ImmutableMarginOrdersPage.class)
public interface MarginOrdersPage {
    @Nullable
    Long lastId();

    @Nullable
    List<OrderResponse> items();
}
