/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.response.KucoinResponse;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by chenshiwei on 2019/1/15.
 */
public interface TimeAPIRetrofit {

    @GET("api/v1/timestamp")
    Call<KucoinResponse<Long>> getServerTimeStamp();

}
