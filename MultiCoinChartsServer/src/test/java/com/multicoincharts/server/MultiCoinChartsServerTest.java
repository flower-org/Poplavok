package com.multicoincharts.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiCoinChartsServerTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private int port;
    private MultiCoinChartsServer server;

    @BeforeEach
    void setUp() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        server = new MultiCoinChartsServer("test-site", "127.0.0.1", port);
        server.start().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @Test
    void servesRootIndex() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("root index"));
    }

    @Test
    void servesDirectoryIndex() throws Exception {
        HttpResponse<String> response = get("/sub/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("sub index"));
    }

    @Test
    void redirectsDirectoryWithoutTrailingSlash() throws Exception {
        HttpResponse<String> response = get("/sub?x=1");
        assertEquals(302, response.statusCode());
        assertEquals("/sub/?x=1", response.headers().firstValue("Location").orElse(""));
    }

    @Test
    void servesExtensionlessPathAsHtml() throws Exception {
        HttpResponse<String> response = get("/page");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("page"));
    }

    @Test
    void returns404ForMissingAndHiddenFiles() throws Exception {
        assertEquals(404, get("/missing").statusCode());
        assertEquals(404, get("/missing.css").statusCode());
        assertEquals(404, get("/.hidden").statusCode());
        assertEquals(404, get("/.git/config").statusCode());
    }

    @Test
    void setsContentTypeAndAnswersConditionalRequests() throws Exception {
        HttpResponse<String> response = get("/page.html");
        assertEquals("text/html;charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        String etag = response.headers().firstValue("ETag").orElseThrow();

        HttpResponse<String> notModified = client.send(HttpRequest.newBuilder(uri("/page.html"))
                .header("If-None-Match", etag).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(304, notModified.statusCode());
        assertEquals("", notModified.body());
    }

    @Test
    void answersHeadWithoutBody() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(String.valueOf(get("/").body().length()), response.headers().firstValue("Content-Length").orElse(""));
        assertEquals("", response.body());
    }

    @Test
    void stopReleasesPortAndCanStartAgain() throws Exception {
        assertTrue(server.isRunning());
        server.stop().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertFalse(server.isRunning());
        assertThrows(ConnectException.class, () -> get("/"));

        server.start().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertEquals(200, get("/").statusCode());
    }
}
