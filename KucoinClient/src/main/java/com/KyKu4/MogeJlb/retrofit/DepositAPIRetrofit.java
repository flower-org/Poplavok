/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.request.DepositAddressCreateRequest;
import com.KyKu4.MogeJlb.response.DepositAddressResponse;
import com.KyKu4.MogeJlb.response.DepositResponse;
import com.KyKu4.MogeJlb.response.KucoinResponse;
import com.KyKu4.MogeJlb.response.Pagination;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by chenshiwei on 2019/1/10.
 */
public interface DepositAPIRetrofit {

    @POST("api/v1/deposit-addresses")
    Call<KucoinResponse<DepositAddressResponse>> createDepositAddress(@Body DepositAddressCreateRequest request);

    @GET("api/v1/deposit-addresses")
    Call<KucoinResponse<DepositAddressResponse>> getDepositAddress(@Query("currency") String currency,
                                                                   @Query(("chain")) String chain);

    @GET("api/v1/deposits")
    Call<KucoinResponse<Pagination<DepositResponse>>> getDepositPageList(@Query("currentPage") int currentPage,
                                                                         @Query("pageSize") int pageSize,
                                                                         @Query("currency") String currency,
                                                                         @Query("status") String status,
                                                                         @Query("startAt") Long startAt,
                                                                         @Query("endAt") Long endAt);
}
