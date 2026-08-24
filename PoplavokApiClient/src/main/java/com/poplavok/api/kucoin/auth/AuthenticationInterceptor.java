package com.poplavok.api.kucoin.auth;

import java.io.IOException;

import com.poplavok.api.kucoin.APIConstants;
import com.poplavok.api.kucoin.exception.KucoinApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    private final KucoinCredentialsProvider credentialsProvider;

    /**
     * Constructor of API - keys are provided on demand
     *
     * @param credentialsProvider The provider for API keys.
     */
    public AuthenticationInterceptor(KucoinCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Validation we have API keys set up
     *
     * @throws KucoinApiException in case of any error
     */
    protected void validateCredentials(String apiKey, String secret, String passPhrase) throws KucoinApiException {
        String humanMessage = ". Please check credentials provider";
        if (Strings.isNullOrEmpty(apiKey))
            throw new KucoinApiException("Missing " + APIConstants.USER_API_KEY + humanMessage);
        if (Strings.isNullOrEmpty(secret))
            throw new KucoinApiException("Missing " + APIConstants.USER_API_SECRET + humanMessage);
        if (Strings.isNullOrEmpty(passPhrase))
            throw new KucoinApiException("Missing " + APIConstants.USER_API_PASSPHRASE + humanMessage);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Integer authApiKeyVersion = credentialsProvider.getApiKeyVersion();
        
        Request original = chain.request();
        Request.Builder newRequestBuilder = original.newBuilder();

        // Version number of api-key
        if (authApiKeyVersion == 1) {
            throw new RuntimeException("KC-API-KEY-VERSION 1 is not supported. Please use version 2.");
        } else if (authApiKeyVersion == 2) {
            String encryptPassPhrase = credentialsProvider.getEncryptPassPhrase();
            newRequestBuilder.addHeader(APIConstants.API_HEADER_PASSPHRASE, encryptPassPhrase);
            newRequestBuilder.addHeader(APIConstants.API_HEADER_KEY_VERSION, authApiKeyVersion.toString());
        } else {
            throw new KucoinApiException("KC-API-KEY-VERSION can only be 1 and 2");
        }

        String apiKey = credentialsProvider.getApiKey();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = credentialsProvider.genSignature(original, timestamp);

        newRequestBuilder.addHeader(APIConstants.API_HEADER_KEY, apiKey);
        newRequestBuilder.addHeader(APIConstants.API_HEADER_SIGN, signature);
        newRequestBuilder.addHeader(APIConstants.API_HEADER_TIMESTAMP, timestamp);
        newRequestBuilder.addHeader(APIConstants.API_HEADER_USER_AGENT, "KuCoin-Java-SDK:" + authApiKeyVersion);
        newRequestBuilder.addHeader("X-VERSION", "default"); // just for dev test

        // Build new request after adding the necessary authentication information
        Request newRequest = newRequestBuilder.build();
        return chain.proceed(newRequest);
    }
}
