package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.response.CurrencyExtendedInfoResponse;
import com.KyKu4.MogeJlb.response.KucoinResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface KucoinAPIRetrofit {

    @GET(value = "_api/pool-info/currency/info")
    Call<KucoinResponse<CurrencyExtendedInfoResponse>> getExtendedInfo(@Query("symbol") String symbol);

}
