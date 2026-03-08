/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Created by zicong.lu on 2018/12/14.
 */
@Value.Immutable
@JsonSerialize(as = ImmutablePageRequest.class)
@JsonDeserialize(as = ImmutablePageRequest.class)
public interface PageRequest {

    default Integer currentPage() {
        return 1;
    }

    default Integer pageSize() {
        return 10;
    }
}
