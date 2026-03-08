package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableSubAccountUserInfoV1.class)
@JsonDeserialize(as = ImmutableSubAccountUserInfoV1.class)
public interface SubAccountUserInfoV1 {
    @Nullable
    String uid();
    /** The user ID of your sub-account */
    @Nullable
    String userId();
    /** The username of your sub-account */
    @Nullable
    String subName();
    /** The type of your sub-account */
    @Nullable
    String type();
    /** Remark */
    @Nullable
    String remarks();
    /** Permission */
    @Nullable
    String access();
}
