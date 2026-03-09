package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.response.CurrencyExtendedInfoResponse;
import com.KyKu4.MogeJlb.response.KucoinResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface KucoinAPIRetrofit {

    @GET(value = "_api/quicksilver/universe-currency/symbols/info/{pSymbol}")
    Call<KucoinResponse<CurrencyExtendedInfoResponse>> getExtendedInfo(@Path("pSymbol") String pSymbol, @Query("symbol") String symbol);

}
