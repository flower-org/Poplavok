package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.exception.KucoinApiException;
import com.KyKu4.MogeJlb.response.KucoinResponse;
import com.KyKu4.MogeJlb.response.WebsocketTokenResponse;
import com.KyKu4.MogeJlb.retrofit.CurrencyAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.KucoinAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.SymbolAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.WebsocketPublicAPIRetrofit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

public class KyKu4_XTTn {
    public static final ObjectMapper OBJECT_MAPPER;
    public static final HttpLoggingInterceptor LOGGING_INTERCEPTOR = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
        @Override
        public void log(String message) {
            System.out.println(message);
        }
    });
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(new GuavaModule());
        LOGGING_INTERCEPTOR.setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    public static final Integer API_KEY_VERSION = 2;
    public static final String BASE_URL = "https://openapi-v2.kucoin.com/";
    public static final String BASE_KUCOIN_URL = "https://www.kucoin.com/";
    public static final Converter.Factory JACKSON_CONVERTER_FACTORY = JacksonConverterFactory.create(OBJECT_MAPPER);

    private static final OkHttpClient HTTP_CLIENT = buildHttpClient();
    private static final Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(JACKSON_CONVERTER_FACTORY)
            .client(HTTP_CLIENT)
            .build();
    private static final Retrofit RETROFIT_KUCOIN = new Retrofit.Builder()
            .baseUrl(BASE_KUCOIN_URL)
            .addConverterFactory(JACKSON_CONVERTER_FACTORY)
            .client(HTTP_CLIENT)
            .build();
    private static final WebsocketPublicAPIRetrofit WEBSOCKET_PUBLIC_API_RETROFIT = RETROFIT.create(WebsocketPublicAPIRetrofit.class);
    private static final Converter<ResponseBody, ?> ERROR_BODY_CONVERTER = JACKSON_CONVERTER_FACTORY.responseBodyConverter(KucoinResponse.class, new Annotation[0], null);
    private static final String KU_SUCCESS_CODE = "200000";
    private static final String HTTP_SUCCESS_CODE = "200";

    private static final PublicAPIPack PUBLIC_API_PACK = ImmutablePublicAPIPack.builder()
                .currencyAPI(RETROFIT.create(CurrencyAPIRetrofit.class))
                .symbolAPI(RETROFIT.create(SymbolAPIRetrofit.class))
                .kucoinAPI(RETROFIT_KUCOIN.create(KucoinAPIRetrofit.class))
                .build();

    public static boolean isSuccessful(String code) {
        return KU_SUCCESS_CODE.equals(code) || HTTP_SUCCESS_CODE.equals(code);
    }

    public static OkHttpClient buildHttpClient() {
        return buildHttpClient(null);
    }

    public static OkHttpClient buildHttpClient(@Nullable Interceptor interceptor) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(100);
        dispatcher.setMaxRequests(100);
        OkHttpClient.Builder builder = (new OkHttpClient.Builder()).dispatcher(dispatcher);
        if (interceptor != null) {
            builder.addInterceptor(interceptor);
        }
        builder.addInterceptor(LOGGING_INTERCEPTOR);

        return builder.build();
    }

    public static <R> R executeSyncAny(Call<R> call) throws IOException {
        Response<R> response = call.execute();
        return response.body();
    }

    public static <R> R executeSync(Call<KucoinResponse<R>> call) throws IOException {
        return Preconditions.checkNotNull(executeSyncNullable(call));
    }

    @Nullable
    public static <R> R executeSyncNullable(Call<KucoinResponse<R>> call) throws IOException {
        Response<KucoinResponse<R>> response = call.execute();
        if (response.isSuccessful() && response.body() != null && isSuccessful(Preconditions.checkNotNull(response.body().code()))) {
            return response.body().data();
        } else {
            KucoinResponse errorResponse;
            if (response.isSuccessful()) {
                errorResponse = response.body();
            } else {
                errorResponse = getErrorResponse(response);
            }

            throw new KucoinApiException(Preconditions.checkNotNull(errorResponse.code()), errorResponse.msg());
        }
    }

    public static KucoinResponse<?> getErrorResponse(Response<?> response) throws IOException {
        return (KucoinResponse) ERROR_BODY_CONVERTER.convert(response.errorBody());
    }

    public static WebsocketTokenResponse getPublicToken() throws IOException {
        return executeSync(WEBSOCKET_PUBLIC_API_RETROFIT.getPublicToken());
    }

    public static WebSocket newWebSocket(Request request, KyKu4_CJlywaTeJlb_Public cJlywaTeJlb_Public, KyKu4_CJlywaTeJlb_CoeguHeHuR cJlywaTeJlb_CoeguHeHuR) {
        return HTTP_CLIENT.newWebSocket(request, new KyKu4_O6pa6oT4uK_OTBeToB(cJlywaTeJlb_Public, cJlywaTeJlb_CoeguHeHuR));
    }

    public static WebSocket newWebSocket(Request request, KyKu4_CJlywaTeJlb_Private cJlywaTeJlb_private, KyKu4_CJlywaTeJlb_CoeguHeHuR cJlywaTeJlb_CoeguHeHuR) {
        return HTTP_CLIENT.newWebSocket(request, new KyKu4_O6pa6oT4uK_OTBeToB(cJlywaTeJlb_private, cJlywaTeJlb_CoeguHeHuR));
    }

    public static PublicAPIPack getPublicAPIPack() {
        return PUBLIC_API_PACK;
    }

    public static void closeHttpClient() {
        HTTP_CLIENT.connectionPool().evictAll();
    }
}
