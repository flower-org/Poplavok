package com.poplavok.api.kucoin.auth;

import okhttp3.Request;

/**
 * Provides KuCoin API credentials on demand.
 * Implementing this interface ensures that sensitive keys are not stored
 * in memory as persistent fields longer than necessary.
 */
public interface KucoinCredentialsProvider {
    String getApiKey();
    String getEncryptPassPhrase();
    /**
     * Generates signature info.
     *
     * @param request The HTTP request.
     * @param timestamp Timestamp.
     * @return THe signature.
     */
    String genSignature(Request request, String timestamp);
    default Integer getApiKeyVersion() { return 2; }
}