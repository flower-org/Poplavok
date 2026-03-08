/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.request.WithdrawApplyRequest;
import com.KyKu4.MogeJlb.response.KucoinResponse;
import com.KyKu4.MogeJlb.response.Pagination;
import com.KyKu4.MogeJlb.response.WithdrawApplyResponse;
import com.KyKu4.MogeJlb.response.WithdrawQuotaResponse;
import com.KyKu4.MogeJlb.response.WithdrawResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by chenshiwei on 2019/1/10.
 */
public interface WithdrawalAPIRetrofit {

    @GET("api/v1/withdrawals/quotas")
    Call<KucoinResponse<WithdrawQuotaResponse>> getWithdrawQuotas(@Query("currency") String currency,
                                                                  @Query("chain") String chain);

    @POST("api/v1/withdrawals")
    Call<KucoinResponse<WithdrawApplyResponse>> applyWithdraw(@Body WithdrawApplyRequest request);

    @DELETE("api/v1/withdrawals/{withdrawalId}")
    Call<KucoinResponse<Void>> cancelWithdraw(@Path("withdrawalId") String withdrawalId);

    @GET("api/v1/withdrawals")
    Call<KucoinResponse<Pagination<WithdrawResponse>>> getWithdrawPageList(@Query("currentPage") int currentPage,
                                                                           @Query("pageSize") int pageSize,
                                                                           @Query("currency") String currency,
                                                                           @Query("status") String status,
                                                                           @Query("startAt") Long startAt,
                                                                           @Query("endAt") Long endAt);
}
