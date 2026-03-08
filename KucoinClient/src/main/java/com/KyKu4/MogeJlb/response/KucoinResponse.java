/**
 * Copyright 2019 Mek Global Limited.
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by zicong.lu on 2018/12/14.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableKucoinResponse.class)
@JsonDeserialize(as = ImmutableKucoinResponse.class)
public interface KucoinResponse<R> //implements Serializable
{
    //private static final long serialVersionUID = 1L;
    @Nullable
    String code();

    default String msg() { return ""; }

    @Nullable
    R data();
}
