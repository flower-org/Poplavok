package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by Reeta on 2019-05-20
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableSubAccountBalanceResponse.class)
@JsonDeserialize(as = ImmutableSubAccountBalanceResponse.class)
public interface SubAccountBalanceResponse {

    @Nullable
    String subUserId();

    @Nullable
    String subName();

    @Nullable
    List<AccountBalancesResponse> tradeAccounts();

    @Nullable
    List<AccountBalancesResponse> mainAccounts();
}
