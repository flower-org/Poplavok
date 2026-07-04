package com.poplavok.kucoin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KuCoinApiSettingsTest {

    @Test
    public void testYamlDeserialization() throws IOException {
        String yamlContent = "apiKey: \"dummy-api-key-1234567890abcdef\"\n" +
                             "secret: \"dummy-secret-key-fedcba0987654321\"\n" +
                             "passPhrase: \"dummy-passphrase-super-secret\"\n";

        byte[] outBytes = yamlContent.getBytes(StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        
        KuCoinApiSettings map = mapper.readValue(outBytes, KuCoinApiSettings.class);

        assertNotNull(map);
        assertEquals("dummy-api-key-1234567890abcdef", map.getApiKey());
        assertEquals("dummy-secret-key-fedcba0987654321", map.getSecret());
        assertEquals("dummy-passphrase-super-secret", map.getPassPhrase());
    }
}
