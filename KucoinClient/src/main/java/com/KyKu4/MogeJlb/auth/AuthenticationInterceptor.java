package com.KyKu4.MogeJlb.auth;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.KyKu4.MogeJlb.APIConstants;
import com.KyKu4.MogeJlb.exception.KucoinApiException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

import javax.annotation.Nullable;

public class AuthenticationInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private final String apiKey;
    private final String secret;
    private final String passPhrase;
    private final Integer authApiKeyVersion;

    /**
     * Constructor of API - keys are loaded from VM options, environment variables, resource files
     *
     * @param apiKey The API key.
     * @param secret The API secret.
     * @param passPhrase The API passphrase.
     * @throws KucoinApiException in case of any error
     */
    public AuthenticationInterceptor(String apiKey, String secret, String passPhrase, Integer authApiKeyVersion) {
        this.apiKey = apiKey;
        this.secret = secret;
        this.passPhrase = passPhrase;
        this.authApiKeyVersion = authApiKeyVersion;
    }

    /**
     * Validation we have API keys set up
     *
     * @throws KucoinApiException in case of any error
     */
    protected void validateCredentials() throws KucoinApiException {
        String humanMessage = ". Please check environment variables or VM options";
        if (Strings.isNullOrEmpty(this.apiKey))
            throw new KucoinApiException("Missing " + APIConstants.USER_API_KEY + humanMessage);
        if (Strings.isNullOrEmpty(this.secret))
            throw new KucoinApiException("Missing " + APIConstants.USER_API_SECRET + humanMessage);
        if (Strings.isNullOrEmpty(this.passPhrase))
            throw new KucoinApiException("Missing " + APIConstants.USER_API_PASSPHRASE + humanMessage);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        validateCredentials();
        Request original = chain.request();
        Request.Builder newRequestBuilder = original.newBuilder();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = genSignature(original, secret, timestamp);

        String encryptPassPhrase = Base64.encodeBase64String(HmacUtils.hmacSha256(secret, passPhrase));

        // Version number of api-key
        if (authApiKeyVersion == 1) {
            newRequestBuilder.addHeader(APIConstants.API_HEADER_PASSPHRASE, passPhrase);
        } else if (authApiKeyVersion == 2) {
            newRequestBuilder.addHeader(APIConstants.API_HEADER_PASSPHRASE, encryptPassPhrase);
            newRequestBuilder.addHeader(APIConstants.API_HEADER_KEY_VERSION, authApiKeyVersion.toString());
        } else {
            throw new KucoinApiException("KC-API-KEY-VERSION can only be 1 and 2");
        }

        newRequestBuilder.addHeader(APIConstants.API_HEADER_KEY, apiKey);
        newRequestBuilder.addHeader(APIConstants.API_HEADER_SIGN, signature);
        newRequestBuilder.addHeader(APIConstants.API_HEADER_TIMESTAMP, timestamp);
        newRequestBuilder.addHeader(APIConstants.API_HEADER_USER_AGENT, "KuCoin-Java-SDK:" + authApiKeyVersion);
        newRequestBuilder.addHeader("X-VERSION", "default"); // just for dev test

        // Build new request after adding the necessary authentication information
        Request newRequest = newRequestBuilder.build();
        return chain.proceed(newRequest);
    }

    /**
     * Generates signature info.
     *
     * @param request The HTTP request.
     * @param apiSecret API secret.
     * @param timestamp Timestamp.
     * @return THe signature.
     */
    public static String genSignature(Request request, String apiSecret, String timestamp) {
        String endpoint = request.url().encodedPath();
        String requestUriParams = request.url().query();
        String requestBody = getRequestBody(request);

        String originToSign = timestamp +
                request.method() +
                endpoint +
                (StringUtils.isBlank(requestUriParams) ? "" : "?" + requestUriParams) +
                (StringUtils.isBlank(requestBody) ? "" : "" + requestBody);

        String signature = Base64.encodeBase64String(HmacUtils.hmacSha256(apiSecret, originToSign));

        LOGGER.debug("originToSign={}", originToSign);
        LOGGER.debug("method={},endpoint={}", request.method(), endpoint);
        LOGGER.debug("signature={}", signature);

        return signature;
    }

    /**
     * Get http request body info.
     *
     * @param request The request
     * @return The request body.
     */
    @Nullable
    public static String getRequestBody(Request request) {
        if (request.body() == null) {
            return null;
        }
        Buffer buffer = new Buffer();
        try {
            request.body().writeTo(buffer);
        } catch (IOException e) {
            throw new RuntimeException("I/O error fetching request body", e);
        }

        //Encoding set to UTF-8
        Charset charset = StandardCharsets.UTF_8;
        MediaType contentType = request.body().contentType();
        if (contentType != null) {
            //???
            charset = contentType.charset(StandardCharsets.UTF_8);
        }

        return buffer.readString(charset);
    }
}
