/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.request.BorrowRequest;
import com.KyKu4.MogeJlb.request.LendRequest;
import com.KyKu4.MogeJlb.request.RepayAllRequest;
import com.KyKu4.MogeJlb.request.RepaySingleRequest;
import com.KyKu4.MogeJlb.request.ToggleAutoLendRequest;
import com.KyKu4.MogeJlb.response.ActiveLendItem;
import com.KyKu4.MogeJlb.response.BorrowOutstandingResponse;
import com.KyKu4.MogeJlb.response.BorrowQueryResponse;
import com.KyKu4.MogeJlb.response.BorrowRepaidResponse;
import com.KyKu4.MogeJlb.response.BorrowResponse;
import com.KyKu4.MogeJlb.response.DoneLendItem;
import com.KyKu4.MogeJlb.response.KucoinResponse;
import com.KyKu4.MogeJlb.response.LastTradeResponse;
import com.KyKu4.MogeJlb.response.LendAssetsResponse;
import com.KyKu4.MogeJlb.response.LendResponse;
import com.KyKu4.MogeJlb.response.MarketItemResponse;
import com.KyKu4.MogeJlb.response.Pagination;
import com.KyKu4.MogeJlb.response.SettledTradeItem;
import com.KyKu4.MogeJlb.response.UnsettledTradeItem;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/**
 * Created by ezreal on 2020/12/08.
 */
public interface LoanAPIRetrofit {

    @POST("api/v1/margin/borrow")
    Call<KucoinResponse<BorrowResponse>> borrow(@Body BorrowRequest request);

    @GET("api/v1/margin/borrow")
    Call<KucoinResponse<BorrowQueryResponse>> queryBorrow(@Query("orderId") String orderId);

    @GET("api/v1/margin/borrow/outstanding")
    Call<KucoinResponse<Pagination<BorrowOutstandingResponse>>> queryBorrowOutstanding(
            @Query("currency") String currency,
            @Query("currentPage") Integer currentPage,
            @Query("pageSize") Integer pageSize);

    @GET("api/v1/margin/borrow/repaid")
    Call<KucoinResponse<Pagination<BorrowRepaidResponse>>> queryBorrowRepaid(
            @Query("currency") String currency,
            @Query("currentPage") Integer currentPage,
            @Query("pageSize") Integer pageSize);

    @POST("api/v1/margin/repay/all")
    Call<KucoinResponse<Void>> repayAll(@Body RepayAllRequest request);

    @POST("api/v1/margin/repay/single")
    Call<KucoinResponse<Void>> repaySingle(@Body RepaySingleRequest request);

    @POST("api/v1/margin/lend")
    Call<KucoinResponse<LendResponse>> lend(@Body LendRequest request);

    @DELETE("api/v1/margin/lend/{orderId}")
    Call<KucoinResponse<Void>> cancelLendOrder(@Path("orderId") String orderId);

    @POST("api/v1/margin/toggle-auto-lend")
    Call<KucoinResponse<Void>> toggleAutoLend(@Body ToggleAutoLendRequest request);

    @GET("api/v1/margin/lend/active")
    Call<KucoinResponse<Pagination<ActiveLendItem>>> queryActiveLend(
            @Query("currency") String currency,
            @Query("currentPage") Integer currentPage,
            @Query("pageSize") Integer pageSize);

    @GET("api/v1/margin/lend/done")
    Call<KucoinResponse<Pagination<DoneLendItem>>> queryDoneLend(
            @Query("currency") String currency,
            @Query("currentPage") Integer currentPage,
            @Query("pageSize") Integer pageSize);

    @GET("api/v1/margin/lend/trade/unsettled")
    Call<KucoinResponse<Pagination<UnsettledTradeItem>>> queryUnsettledTrade(
            @Query("currency") String currency,
            @Query("currentPage") Integer currentPage,
            @Query("pageSize") Integer pageSize);

    @GET("api/v1/margin/lend/trade/settled")
    Call<KucoinResponse<Pagination<SettledTradeItem>>> querySettledTrade(
            @Query("currency") String currency,
            @Query("currentPage") Integer currentPage,
            @Query("pageSize") Integer pageSize);

    @GET("api/v1/margin/lend/assets")
    Call<KucoinResponse<List<LendAssetsResponse>>> queryLendAssets(@Query("currency") String currency);

    @GET("api/v1/margin/market")
    Call<KucoinResponse<List<MarketItemResponse>>> queryMarket(
            @Query("currency") String currency,
            @Query("term") Integer term);

    @GET("api/v1/margin/trade/last")
    Call<KucoinResponse<List<LastTradeResponse>>> queryLastTrade(@Query("currency") String currency);

}
