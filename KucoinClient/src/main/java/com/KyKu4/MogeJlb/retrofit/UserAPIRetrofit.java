package com.KyKu4.MogeJlb.retrofit;

import com.KyKu4.MogeJlb.response.KucoinResponse;
import com.KyKu4.MogeJlb.response.SubUserInfoResponse;
import retrofit2.Call;
import retrofit2.http.GET;

import java.util.List;

/**
 * Created by Reeta on 2019-05-20
 */
public interface UserAPIRetrofit {

    @GET("api/v1/sub/user")
    Call<KucoinResponse<List<SubUserInfoResponse>>> getSubUserList();
}
