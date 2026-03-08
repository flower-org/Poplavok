package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.auth.AuthenticationInterceptor;
import com.KyKu4.MogeJlb.response.WebsocketTokenResponse;
import com.KyKu4.MogeJlb.retrofit.AccountAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.CurrencyAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.LoanAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.MarginAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.OrderAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.SymbolAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.WebsocketAuthAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.WithdrawalAPIRetrofit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static com.KyKu4.npo4ee.KyKu4_XTTn.API_KEY_VERSION;
import static com.KyKu4.npo4ee.KyKu4_XTTn.BASE_URL;
import static com.KyKu4.npo4ee.KyKu4_XTTn.JACKSON_CONVERTER_FACTORY;
import static com.KyKu4.npo4ee.KyKu4_XTTn.buildHttpClient;
import static com.KyKu4.npo4ee.KyKu4_XTTn.executeSync;

public class KyKu4_npuBaT_XTTn {
    public static WebsocketTokenResponse getPrivateToken(WebsocketAuthAPIRetrofit authApi) throws IOException {
        return executeSync(authApi.getPrivateToken());
    }

    private static OkHttpClient buildPrivateHttpClient(String apiKey, String secret, String passPhrase, Integer authApiKeyVersion) {
        return buildHttpClient(new AuthenticationInterceptor(apiKey, secret, passPhrase, authApiKeyVersion));
    }

    static ConcurrentHashMap<String, PrivateAPIPack> apiPackCache = new ConcurrentHashMap<>();

    public static PrivateAPIPack getAPIPack(String apiKey, String secret, String passPhrase) {
        String packId = apiKey + secret + passPhrase;

        PrivateAPIPack apiPack = apiPackCache.get(packId);
        while (apiPack == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(JACKSON_CONVERTER_FACTORY)
                    .client(buildPrivateHttpClient(apiKey, secret, passPhrase, API_KEY_VERSION))
                    .build();

            apiPack = ImmutablePrivateAPIPack.builder()
                    .authAPI(retrofit.create(WebsocketAuthAPIRetrofit.class))
                    .accountAPI(retrofit.create(AccountAPIRetrofit.class))
                    .marginAPI(retrofit.create(MarginAPIRetrofit.class))
                    .loanAPI(retrofit.create(LoanAPIRetrofit.class))
                    .withdrawalAPI(retrofit.create(WithdrawalAPIRetrofit.class))
                    .orderAPI(retrofit.create(OrderAPIRetrofit.class))
                    .build();
            PrivateAPIPack previousPack = apiPackCache.putIfAbsent(packId, apiPack);
            if (previousPack == null) {
                break;
            } else {
                apiPack = previousPack;
            }
        }

        return apiPack;
    }
}
