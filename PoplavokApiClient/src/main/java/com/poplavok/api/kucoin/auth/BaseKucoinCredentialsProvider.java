package com.poplavok.api.kucoin.auth;

import okhttp3.MediaType;
import okhttp3.Request;
import okio.Buffer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class BaseKucoinCredentialsProvider implements KucoinCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseKucoinCredentialsProvider.class);

    protected abstract String getSecret();
    protected abstract String getPassPhrase();

    @Override
    public String getEncryptPassPhrase() {
        return Base64.encodeBase64String(HmacUtils.hmacSha256(getSecret(), getPassPhrase()));
    }

    @Override
    public String genSignature(Request request, String timestamp) {
        String endpoint = request.url().encodedPath();
        String requestUriParams = request.url().query();
        String requestBody = getRequestBody(request);

        String originToSign = timestamp +
                request.method() +
                endpoint +
                (StringUtils.isBlank(requestUriParams) ? "" : "?" + requestUriParams) +
                (StringUtils.isBlank(requestBody) ? "" : requestBody);

        String signature = Base64.encodeBase64String(HmacUtils.hmacSha256(getSecret(), originToSign));

        /*LOGGER.debug("originToSign={}", originToSign);
        LOGGER.debug("method={},endpoint={}", request.method(), endpoint);
        LOGGER.debug("signature={}", signature);*/

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
