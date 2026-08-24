package com.poplavok.api.kucoin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.poplavok.api.kucoin.auth.AuthenticationInterceptor;
import com.poplavok.api.kucoin.auth.KucoinCredentialsProvider;
import com.poplavok.api.kucoin.exception.KucoinApiException;
import com.poplavok.api.kucoin.model.request.OrderCreateRequest;
import com.poplavok.api.kucoin.model.response.AccountBalancesResponse;
import com.poplavok.api.kucoin.model.response.AllTickersResponse;
import com.poplavok.api.kucoin.model.response.CancelOrderResponse;
import com.poplavok.api.kucoin.model.response.CurrencyDetailV2Response;
import com.poplavok.api.kucoin.model.response.CurrencyExtendedInfoResponse;
import com.poplavok.api.kucoin.model.response.CurrencyResponse;
import com.poplavok.api.kucoin.model.response.IsolatedMarginAccountInfo;
import com.poplavok.api.kucoin.model.response.KucoinResponse;
import com.poplavok.api.kucoin.model.response.MarginAccountResponse;
import com.poplavok.api.kucoin.model.response.OrderCreateResponse;
import com.poplavok.api.kucoin.model.response.OrderResponse;
import com.poplavok.api.kucoin.model.response.Pagination;
import com.poplavok.api.kucoin.model.response.WebsocketTokenResponse;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Proxy;
import java.util.List;

public class KucoinApiClient {
    private static final String BASE_URL = "https://api.kucoin.com/";
    private static final String BASE_KUCOIN_URL = "https://www.kucoin.com/";

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public KucoinApiClient(@Nullable KucoinCredentialsProvider credentialsProvider, @Nullable Proxy proxy) {
        this.mapper = new ObjectMapper();
        this.mapper.registerModules(new GuavaModule());
        
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (credentialsProvider != null) {
            builder.addInterceptor(new AuthenticationInterceptor(credentialsProvider));
        }
        if (proxy != null) {
            builder.proxy(proxy);
        }
        this.httpClient = builder.build();
    }

    public KucoinApiClient(@Nullable KucoinCredentialsProvider credentialsProvider) {
        this(credentialsProvider, null);
    }

    public KucoinApiClient() {
        this(null);
    }

    private <T> T execute(Request request, TypeReference<KucoinResponse<T>> typeRef) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new KucoinApiException(String.valueOf(response.code()), response.message());
            }
            if (response.body() == null) {
                throw new KucoinApiException(String.valueOf(response.code()), "Empty body");
            }
            KucoinResponse<T> kucoinResponse = mapper.readValue(response.body().string(), typeRef);
            if (!"200000".equals(kucoinResponse.code()) && !"200".equals(kucoinResponse.code())) {
                throw new KucoinApiException(kucoinResponse.code() != null ? kucoinResponse.code() : "UNKNOWN", kucoinResponse.msg());
            }
            if (kucoinResponse.data() == null) {
                throw new KucoinApiException("Empty Data", "Data is null");
            }
            return kucoinResponse.data();
        }
    }

    public List<CurrencyResponse> getCurrencies() throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "api/ua/v1/asset/currencies").get().build();
        return execute(request, new TypeReference<KucoinResponse<List<CurrencyResponse>>>() {});
    }

    public CurrencyDetailV2Response getCurrencyDetailV2(String currency) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "api/ua/v1/market/currency").newBuilder();
        if (currency != null) urlBuilder.addQueryParameter("currency", currency);
        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        return execute(request, new TypeReference<KucoinResponse<CurrencyDetailV2Response>>() {});
    }

    public CurrencyExtendedInfoResponse getCurrencyExtendedInfo(String currency) throws IOException {
        Request request = new Request.Builder().url(BASE_KUCOIN_URL + "_api/quicksilver/universe-currency/symbols/info/" + currency).get().build();
        return execute(request, new TypeReference<KucoinResponse<CurrencyExtendedInfoResponse>>() {});
    }

    public List<AccountBalancesResponse> getAccountList(@Nullable String currency, @Nullable String type) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "api/v1/accounts").newBuilder();
        if (currency != null) urlBuilder.addQueryParameter("currency", currency);
        if (type != null) urlBuilder.addQueryParameter("type", type);
        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        return execute(request, new TypeReference<KucoinResponse<List<AccountBalancesResponse>>>() {});
    }

    public Pagination<OrderResponse> getOrderList(@Nullable String tradeType, @Nullable String status, @Nullable Integer pageSize, @Nullable Integer currentPage) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "api/v1/orders").newBuilder();
        if (tradeType != null) urlBuilder.addQueryParameter("tradeType", tradeType);
        if (status != null) urlBuilder.addQueryParameter("status", status);
        if (pageSize != null) urlBuilder.addQueryParameter("pageSize", String.valueOf(pageSize));
        if (currentPage != null) urlBuilder.addQueryParameter("currentPage", String.valueOf(currentPage));
        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        return execute(request, new TypeReference<KucoinResponse<Pagination<OrderResponse>>>() {});
    }

    public OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest) throws IOException {
        String json = mapper.writeValueAsString(orderCreateRequest);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(BASE_URL + "api/v1/orders").post(body).build();
        return execute(request, new TypeReference<KucoinResponse<OrderCreateResponse>>() {});
    }

    public OrderCreateResponse createMarginOrder(OrderCreateRequest orderCreateRequest) throws IOException {
        String json = mapper.writeValueAsString(orderCreateRequest);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(BASE_URL + "api/v1/margin/order").post(body).build();
        return execute(request, new TypeReference<KucoinResponse<OrderCreateResponse>>() {});
    }

    public OrderResponse getOrder(String orderId) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "api/v1/orders/" + orderId).get().build();
        return execute(request, new TypeReference<KucoinResponse<OrderResponse>>() {});
    }

    public CancelOrderResponse cancelOrder(String orderId) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "api/v1/orders/" + orderId).delete().build();
        return execute(request, new TypeReference<KucoinResponse<CancelOrderResponse>>() {});
    }

    public MarginAccountResponse getMarginAccount() throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "api/v1/margin/account").get().build();
        return execute(request, new TypeReference<KucoinResponse<MarginAccountResponse>>() {});
    }

    public IsolatedMarginAccountInfo getIsolatedMarginAccountInfo(@Nullable String balanceCurrency) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "api/v1/isolated/accounts").newBuilder();
        if (balanceCurrency != null) urlBuilder.addQueryParameter("balanceCurrency", balanceCurrency);
        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        return execute(request, new TypeReference<KucoinResponse<IsolatedMarginAccountInfo>>() {});
    }

    public AllTickersResponse getAllTickers() throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "api/v1/market/allTickers").get().build();
        return execute(request, new TypeReference<KucoinResponse<AllTickersResponse>>() {});
    }

    public WebsocketTokenResponse getPublicToken() throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "api/v1/bullet-public").post(RequestBody.create(new byte[0], null)).build();
        return execute(request, new TypeReference<KucoinResponse<WebsocketTokenResponse>>() {});
    }
}
