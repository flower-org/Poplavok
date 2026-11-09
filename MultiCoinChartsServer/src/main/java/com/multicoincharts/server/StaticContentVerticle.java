package com.multicoincharts.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves the MultiCoinCharts static site from a classpath directory at the server root.
 * Resources are read straight from the classpath into memory; nothing is written to the file system.
 */
public class StaticContentVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticContentVerticle.class);

    private final String webRoot;
    private final String host;
    private final int port;
    // Only resources that exist are cached, so memory use is bounded by the size of the site.
    private final Map<String, Resource> cache = new ConcurrentHashMap<>();

    private static final class Resource {
        final Buffer body;
        final String contentType;
        final String etag;

        Resource(Buffer body, String contentType, String etag) {
            this.body = body;
            this.contentType = contentType;
            this.etag = etag;
        }
    }

    public StaticContentVerticle(String webRoot, String host, int port) {
        this.webRoot = webRoot;
        this.host = host;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().method(HttpMethod.GET).method(HttpMethod.HEAD).handler(this::serve);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, host)
                .onSuccess(server -> LOGGER.info("Serving '{}' at http://{}:{}/", webRoot, host, server.actualPort()))
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

    private void serve(RoutingContext ctx) {
        String path = ctx.normalizedPath();
        if (!path.startsWith("/") || isHidden(path)) {
            ctx.next();
        } else if (path.endsWith("/")) {
            send(ctx, path + "index.html");
        } else if (lastSegment(path).contains(".")) {
            send(ctx, path);
        } else {
            // Extensionless path: either a directory (/home -> /home/, so that relative links in its index.html
            // resolve against the directory) or a page linked without extension (/adv-scanner -> adv-scanner.html).
            load(path + "/index.html").onComplete(dirIndex -> {
                if (dirIndex.succeeded() && dirIndex.result().isPresent()) {
                    String query = ctx.request().query();
                    ctx.redirect(path + "/" + (query == null ? "" : "?" + query));
                } else {
                    send(ctx, path + ".html");
                }
            });
        }
    }

    private void send(RoutingContext ctx, String path) {
        load(path).onComplete(loaded -> {
            if (loaded.failed()) {
                ctx.fail(loaded.cause());
                return;
            }
            Optional<Resource> resource = loaded.result();
            if (resource.isEmpty()) {
                ctx.next();
                return;
            }
            Resource r = resource.get();
            ctx.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, r.contentType)
                    .putHeader(HttpHeaders.ETAG, r.etag);
            if (r.etag.equals(ctx.request().getHeader(HttpHeaders.IF_NONE_MATCH))) {
                ctx.response().setStatusCode(304).end();
            } else if (HttpMethod.HEAD.equals(ctx.request().method())) {
                ctx.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(r.body.length())).end();
            } else {
                ctx.response().end(r.body);
            }
        });
    }

    private Future<Optional<Resource>> load(String path) {
        Resource cached = cache.get(path);
        if (cached != null) {
            return Future.succeededFuture(Optional.of(cached));
        }
        return vertx.executeBlocking(() -> {
            Optional<Resource> resource = read(webRoot + path);
            resource.ifPresent(r -> cache.put(path, r));
            return resource;
        }, false);
    }

    private static Optional<Resource> read(String name) throws IOException, NoSuchAlgorithmException {
        try (InputStream in = StaticContentVerticle.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                return Optional.empty();
            }
            byte[] bytes = in.readAllBytes();
            String etag = "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes), 0, 8) + "\"";
            return Optional.of(new Resource(Buffer.buffer(bytes), contentType(name), etag));
        }
    }

    private static String contentType(String name) {
        String mimeType = MimeMapping.getMimeTypeForFilename(name);
        if (mimeType == null) {
            return "application/octet-stream";
        }
        return mimeType.startsWith("text/") || mimeType.equals("application/json")
                ? mimeType + ";charset=UTF-8"
                : mimeType;
    }

    /** Any path segment starting with a dot (.git, .gitignore, ...). The path is normalized, so ".." can't occur. */
    private static boolean isHidden(String path) {
        return path.contains("/.");
    }

    private static String lastSegment(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
