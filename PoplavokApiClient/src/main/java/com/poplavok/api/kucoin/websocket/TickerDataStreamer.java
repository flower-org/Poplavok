package com.poplavok.api.kucoin.websocket;

import com.poplavok.api.kucoin.model.response.ImmutableMarketTickerResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.api.kucoin.model.InstanceServer;
import com.poplavok.api.kucoin.model.response.WebsocketTokenResponse;
import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.TickerChangeEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TickerDataStreamer extends WebSocketListener {
    public final static String ALL_TICKERS = "all";

    final static Logger LOGGER = LoggerFactory.getLogger(TickerDataStreamer.class);
    final static Long FIVE_SECONDS_IN_MILLIS = 5 * 1000L;
    final static Long ONE_SECOND_IN_MILLIS = 1000L;
    final static String PONG = "pong";

    final AtomicReference<WebSocket> webSocket = new AtomicReference<>(null);
    final Thread pingThread;

    final Set<String> topics = ConcurrentHashMap.newKeySet();
    final TickerCallback tickerCallback;
    final KucoinApiClient apiClient;
    final OkHttpClient httpClient;
    final ObjectMapper mapper;

    final AtomicBoolean reconnecting = new AtomicBoolean(false);
    final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    final AtomicLong pingCounter = new AtomicLong(0);

    // Holds the socket a ping was sent on and the id of the last unacknowledged ping.
    final AtomicReference<Pair<WebSocket, String>> lastPing = new AtomicReference<>(null);

    public TickerDataStreamer(String topic, TickerCallback tickerCallback) {
        this(topic, tickerCallback, null);
    }

    public TickerDataStreamer(String topic, TickerCallback tickerCallback, @Nullable java.net.Proxy proxy) {
        if (topic != null && !topic.isEmpty()) {
            this.topics.add(topic);
        }
        this.tickerCallback = tickerCallback;
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

                if (socket == null) {
                    // Bootstrap the first connection or recover a missing one.
                    reInit(null, -1, null);
                    // Avoid busy-spinning if another thread already owns the reconnect.
                    try {
                        Thread.sleep(ONE_SECOND_IN_MILLIS);
                    } catch (InterruptedException e) {
                        LOGGER.info("Ping thread interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                try {
                    String pingId = "ping-" + pingCounter.incrementAndGet();
                    String pingMsg = "{\"id\":\"" + pingId + "\",\"type\":\"ping\"}";
                    // Record the pending ping before sending so an immediate pong can match it.
                    lastPing.set(Pair.of(socket, pingId));
                    socket.send(pingMsg);

                    Thread.sleep(FIVE_SECONDS_IN_MILLIS);

                    Pair<WebSocket, String> last = lastPing.get();
                    if (last != null && last.getLeft() == socket && last.getRight().equals(pingId)) {
                        LOGGER.error("Ping confirmation not received, reinitializing connection");
                        reInit(socket, 1002, "Ping confirmation not received");
                    }
                } catch (InterruptedException e) {
                    LOGGER.info("Ping thread interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.info("Ping thread exception", e);
                    reInit(socket, 1002, "Ping thread exception");
                }
            }
        });
    }

    public void start() {
        pingThread.start();
    }

    public void shutdown() {
        shuttingDown.set(true);
        if (pingThread != null) {
            pingThread.interrupt();
        }
        WebSocket socket = webSocket.getAndSet(null);
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
        String msg = "{\"id\":\"" + uuid + "\",\"type\":\"subscribe\",\"topic\":\"/market/ticker:" + topic + "\",\"privateChannel\":false,\"response\":true}";
        socket.send(msg);
    }

    private void sendUnsubscribe(WebSocket socket, String topic) {
        String uuid = UUID.randomUUID().toString();
        String msg = "{\"id\":\"" + uuid + "\",\"type\":\"unsubscribe\",\"topic\":\"/market/ticker:" + topic + "\",\"privateChannel\":false,\"response\":true}";
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
        if (oldSocket != null) {
            // Only the thread that successfully clears the current socket may drive the reconnect.
            if (!webSocket.compareAndSet(oldSocket, null)) {
                return;
            }
            oldSocket.close(code, reason);
        }

        if (shuttingDown.get()) {
            return;
        }

        // Ensure only one reconnect runs at a time.
        if (!reconnecting.compareAndSet(false, true)) {
            return;
        }

        try {
            while (webSocket.get() == null && !shuttingDown.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    WebSocket newWebSocket = initWebSocket();

                    for (String t : topics) {
                        sendSubscribe(newWebSocket, t);
                    }

                    webSocket.set(newWebSocket);
                    break;
                } catch (IOException e) {
                    LOGGER.info("Websocket Re-init failed:", e);
                    try {
                        Thread.sleep(FIVE_SECONDS_IN_MILLIS);
                    } catch (InterruptedException ie) {
                        LOGGER.info("Reconnect wait interrupted", ie);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            reconnecting.set(false);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            KucoinEvent event = mapper.readValue(text, KucoinEvent.class);
            if (PONG.equals(event.type())) {
                // Only clear the pending ping if this pong actually acknowledges it,
                // so a late pong from an earlier cycle can't suppress a needed reconnect.
                Pair<WebSocket, String> last = lastPing.get();
                if (last != null && last.getRight().equals(event.id())) {
                    lastPing.compareAndSet(last, null);
                }
            } else if ("message".equals(event.type())) {
                // we assume it's TickerChangeEvent
                KucoinEvent<TickerChangeEvent> tickerEvent = mapper.readValue(text, mapper.getTypeFactory().constructParametricType(KucoinEvent.class, TickerChangeEvent.class));
                tickerCallback.tickerCallback(tickerEvent);
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
        if (!shuttingDown.get()) {
            reInit(webSocket, code, reason);
        }
    }

    @Override
    public void onFailure(WebSocket socket, Throwable t, @Nullable Response response) {
        LOGGER.info("Web socket: onFailure; exception {}; response {}", t, response);
        if (!shuttingDown.get()) {
            reInit(socket, 1001, t.toString());
        }
    }
}
