package com.multicoincharts.server;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/** Embeddable MultiCoinCharts web server. */
public class MultiCoinChartsServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiCoinChartsServer.class);

    /** Classpath directory holding the site (the multicoincharts-minimal submodule under src/main/resources). */
    public static final String WEB_ROOT = "multicoincharts-minimal";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8000;

    private final String webRoot;
    private final String host;
    private final int port;
    @Nullable private Vertx vertx;

    public MultiCoinChartsServer() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public MultiCoinChartsServer(String host, int port) {
        this(WEB_ROOT, host, port);
    }

    public MultiCoinChartsServer(String webRoot, String host, int port) {
        this.webRoot = webRoot;
        this.host = host;
        this.port = port;
    }

    public synchronized Future<Void> start() {
        if (vertx != null) {
            return Future.failedFuture(new IllegalStateException("Server already started"));
        }
        // Content is served from memory by StaticContentVerticle. With classpath resolving off, Vert.x never creates
        // its file cache dir (./.vertx), so the server performs no file system writes at all.
        Vertx newVertx = Vertx.vertx(new VertxOptions().setFileSystemOptions(new FileSystemOptions()
                .setClassPathResolvingEnabled(false)
                .setFileCachingEnabled(false)));
        vertx = newVertx;

        return newVertx.deployVerticle(new StaticContentVerticle(webRoot, host, port))
                .onFailure(err -> {
                    LOGGER.error("Failed to start MultiCoinCharts server", err);
                    stop();
                })
                .mapEmpty();
    }

    public synchronized Future<Void> stop() {
        Vertx current = vertx;
        vertx = null;
        if (current == null) {
            return Future.succeededFuture();
        }
        return current.close()
                .onSuccess(v -> LOGGER.info("MultiCoinCharts server stopped"))
                .onFailure(err -> LOGGER.error("Error stopping MultiCoinCharts server", err));
    }

    public synchronized boolean isRunning() {
        return vertx != null;
    }
}
