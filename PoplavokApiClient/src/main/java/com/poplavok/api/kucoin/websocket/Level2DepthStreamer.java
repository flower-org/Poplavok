package com.poplavok.api.kucoin.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.api.kucoin.model.InstanceServer;
import com.poplavok.api.kucoin.model.response.WebsocketTokenResponse;
import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.Level2DepthEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Level2DepthStreamer extends WebSocketListener {
    final static Logger LOGGER = LoggerFactory.getLogger(Level2DepthStreamer.class);
    final static Long FIVE_SECONDS_IN_MILLIS = 5 * 1000L;
    final static String PONG = "pong";

    public enum Depth {
        DEPTH_5, DEPTH_50
    }

    final AtomicReference<WebSocket> webSocket = new AtomicReference<>(null);
    final Thread pingThread;

    final Set<String> topics = ConcurrentHashMap.newKeySet();
    final Depth depth;
    final Level2DepthCallback callback;
    final KucoinApiClient apiClient;
    final OkHttpClient httpClient;
    final ObjectMapper mapper;

    final AtomicReference<Pair<WebSocket, Long>> lastPing = new AtomicReference<>(null);

    public Level2DepthStreamer(String topic, Depth depth, Level2DepthCallback callback) {
        this(topic, depth, callback, null);
    }

    public Level2DepthStreamer(String topic, Depth depth, Level2DepthCallback callback, @Nullable java.net.Proxy proxy) {
        if (topic != null && !topic.isEmpty()) {
            this.topics.add(topic);
        }
        this.depth = depth;
        this.callback = callback;
        this.apiClient = new KucoinApiClient(null, proxy);
        
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        if (proxy != null) {
            clientBuilder.proxy(proxy);
        }
        this.httpClient = clientBuilder.build();
        
        this.mapper = new ObjectMapper().registerModules(new GuavaModule());

        pingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                WebSocket socket = webSocket.get();

                if (socket != null) {
                    try {
                        String pingMsg = "{\"id\":\"pingu\",\"type\":\"ping\"}";
                        socket.send(pingMsg);
                        lastPing.set(Pair.of(socket, System.currentTimeMillis()));

                        try {
                            Thread.sleep(FIVE_SECONDS_IN_MILLIS);
                        } catch (InterruptedException e) {
                            LOGGER.info("Ping thread interrupted", e);
                            Thread.currentThread().interrupt();
                            break;
                        }

                        Pair<WebSocket, Long> last = lastPing.get();
                        if (last != null) {
                            if (last.getLeft() == socket) {
                                if (System.currentTimeMillis() - last.getRight() > FIVE_SECONDS_IN_MILLIS) {
                                    LOGGER.error("Ping confirmation not received, reinitializing connection");
                                    reInit(socket, 1002, "Ping confirmation not received");
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.info("Ping thread exception", e);
                        reInit(socket, 1002, "Ping thread exception");
                    }
                } else {
                    reInit(null, -1, null);
                }
            }
        });
    }

    public void start() {
        pingThread.start();
    }

    public void shutdown() {
        if (pingThread != null) {
            pingThread.interrupt();
        }
        WebSocket socket = webSocket.get();
        if (socket != null) {
            socket.close(1000, "Shutting down");
        }
        // Cleanup OkHttpClient resources to allow JVM to exit
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    public void subscribe(String topic) {
        if (topics.add(topic)) {
            WebSocket socket = webSocket.get();
            if (socket != null) {
                sendSubscribe(socket, topic);
            }
        }
    }

    public void unsubscribe(String topic) {
        if (topics.remove(topic)) {
            WebSocket socket = webSocket.get();
            if (socket != null) {
                sendUnsubscribe(socket, topic);
            }
        }
    }

    private void sendSubscribe(WebSocket socket, String topic) {
        String uuid = UUID.randomUUID().toString();
        String prefix = depth == Depth.DEPTH_5 ? "/spotMarket/level2Depth5:" : "/spotMarket/level2Depth50:";
        String msg = "{\"id\":\"" + uuid + "\",\"type\":\"subscribe\",\"topic\":\"" + prefix + topic + "\",\"privateChannel\":false,\"response\":true}";
        socket.send(msg);
    }

    private void sendUnsubscribe(WebSocket socket, String topic) {
        String uuid = UUID.randomUUID().toString();
        String prefix = depth == Depth.DEPTH_5 ? "/spotMarket/level2Depth5:" : "/spotMarket/level2Depth50:";
        String msg = "{\"id\":\"" + uuid + "\",\"type\":\"unsubscribe\",\"topic\":\"" + prefix + topic + "\",\"privateChannel\":false,\"response\":true}";
        socket.send(msg);
    }

    private WebSocket initWebSocket() throws IOException {
        WebsocketTokenResponse websocketToken = apiClient.getPublicToken();
        LOGGER.info("{}", websocketToken);

        InstanceServer instanceServer = websocketToken.instanceServers().get((int) (websocketToken.instanceServers().size() * Math.random()));
        String streamingUrl = instanceServer.endpoint() + "?token=" + websocketToken.token();
        Request request = new Request.Builder().url(streamingUrl).build();

        return httpClient.newWebSocket(request, this);
    }

    private void reInit(@Nullable WebSocket oldSocket, int code, @Nullable String reason) {
        if (webSocket.compareAndSet(oldSocket, null)) {
            if (oldSocket != null) {
                oldSocket.close(code, reason);
            }

            while (true) {
                try {
                    WebSocket newWebSocket = initWebSocket();
                    webSocket.set(newWebSocket);
                    
                    for (String t : topics) {
                        sendSubscribe(newWebSocket, t);
                    }
                    
                    break;
                } catch (IOException e) {
                    LOGGER.info("Websocket Re-init failed:", e);
                    try {
                        Thread.sleep(FIVE_SECONDS_IN_MILLIS);
                    } catch (InterruptedException ie) {
                        LOGGER.info("Ping thread interrupted", ie);
                    }
                }
            }
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            KucoinEvent event = mapper.readValue(text, KucoinEvent.class);
            if (PONG.equals(event.type())) {
                lastPing.set(null);
            } else if ("message".equals(event.type())) {
                KucoinEvent<Level2DepthEvent> depthEvent = mapper.readValue(text, mapper.getTypeFactory().constructParametricType(KucoinEvent.class, Level2DepthEvent.class));
                callback.onCallback(depthEvent);
            } else {
                LOGGER.info("Other message received: type {}", event);
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse message: {}", text, e);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        LOGGER.info("Web socket: onOpen; response {}", response);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        LOGGER.info("Web socket: onClosing; code {}; reason {}", code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        LOGGER.info("Web socket: onClosed; code {}; reason {}", code, reason);
    }

    @Override
    public void onFailure(WebSocket socket, Throwable t, @Nullable Response response) {
        LOGGER.info("Web socket: onFailure; exception {}; response {}", t, response);
        reInit(socket, 1001, t.toString());
    }
}
