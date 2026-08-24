package com.poplavok.kucoin;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableKuCoinApiSettings.class)
@JsonDeserialize(as = ImmutableKuCoinApiSettings.class)
public interface KuCoinApiSettings {
    String getSecret();
    String getPassPhrase();
    String getApiKey();
}
