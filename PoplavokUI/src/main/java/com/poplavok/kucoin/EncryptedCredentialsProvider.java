package com.poplavok.kucoin;

import com.flower.crypt.HybridAesEncryptor;
import com.flower.crypt.keys.KeyProvider;
import com.flower.crypt.keys.RsaKeyContext;
import com.poplavok.api.kucoin.auth.BaseKucoinCredentialsProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;

public class EncryptedCredentialsProvider extends BaseKucoinCredentialsProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    final KeyProvider keyProvider;
    final File credentialsFile;
    final byte[] credentialsFileBytes;

    public EncryptedCredentialsProvider(KeyProvider keyProvider, File credentialsFile) throws IOException {
        this.keyProvider = keyProvider;
        this.credentialsFile = credentialsFile;
        this.credentialsFileBytes = Files.readAllBytes(credentialsFile.toPath());
    }

    @Override
    protected String getSecret() {
        return getSettings(credentialsFileBytes).getSecret();
    }

    @Override
    protected String getPassPhrase() {
        return getSettings(credentialsFileBytes).getPassPhrase();
    }

    @Override
    public String getApiKey() {
        return getSettings(credentialsFileBytes).getApiKey();
    }

    protected KuCoinApiSettings getSettings(byte[] inputBytes) {
        byte[] outBytes = decrypt(inputBytes);
        try {
            return MAPPER.readValue(outBytes, KuCoinApiSettings.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML", e);
        }
    }

    protected byte[] decrypt(byte[] inputBytes) {
        byte[] outBytes;
        try {
            PrivateKey privateKey = ((RsaKeyContext)keyProvider.getKeyContext()).privateKey();
            PublicKey publicKey = ((RsaKeyContext)keyProvider.getKeyContext()).publicKey();

            try (ByteArrayInputStream bis = new ByteArrayInputStream(inputBytes);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                HybridAesEncryptor.decrypt(bis, bos, HybridAesEncryptor.Mode.PUBLIC_KEY_ENCRYPT, privateKey, publicKey, inputBytes.length);
                outBytes = bos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outBytes;
    }
}
