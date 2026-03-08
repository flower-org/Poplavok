/**
 * Copyright 2019 Mek Global Limited.
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by zicong.lu on 2018/12/21.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutablePagination.class)
@JsonDeserialize(as = ImmutablePagination.class)
public interface Pagination<T> {
    int DEFAULT_PAGE_SIZE = 50;

    @Nullable
    Integer currentPage();

    @Nullable
    Integer pageSize();

    @Nullable
    Long totalNum();

    @Nullable
    Integer totalPage();

    @Nullable
    List<T> items();

    /*public Pagination(Integer currentPage, Integer pageSize, Long totalNum, List<T> items) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalNum = totalNum;
        this.items = items;
        this.totalPage = (int)((totalNum + (long)pageSize - 1L) / (long)pageSize);
    }*/

}
