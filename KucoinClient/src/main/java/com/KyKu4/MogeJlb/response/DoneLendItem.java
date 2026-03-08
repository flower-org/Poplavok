/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableDoneLendItem.class)
@JsonDeserialize(as = ImmutableDoneLendItem.class)
public interface DoneLendItem extends BaseLoanItem {

    @Nullable
    String status();
}
