package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by Reeta on 2019-05-20
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSubMasterTransferV2Request.class)
@JsonDeserialize(as = ImmutableSubMasterTransferV2Request.class)
public interface SubMasterTransferV2Request {
    @Nullable
    String clientOid();

    @Nullable
    String currency();

    @Nullable
    BigDecimal amount();

    @Nullable
    String direction();

    @Nullable
    String accountType();

    @Nullable
    String subUserId();

    @Nullable
    String subAccountType();

}
