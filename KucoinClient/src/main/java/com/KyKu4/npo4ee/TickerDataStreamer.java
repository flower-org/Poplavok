package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.model.InstanceServer;
import com.KyKu4.MogeJlb.response.WebsocketTokenResponse;
import com.KyKu4.MogeJlb.websocket.event.KucoinEvent;
import com.KyKu4.MogeJlb.websocket.event.TickerChangeEvent;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class TickerDataStreamer implements KyKu4_CJlywaTeJlb_Public, KyKu4_CJlywaTeJlb_CoeguHeHuR {
    public final static String ALL_TICKERS = "all";

    final static Logger LOGGER = LoggerFactory.getLogger(TickerDataStreamer.class);
    final static Long FIVE_SECONDS_IN_MILLIS = 5 * 1000L;
    final static String PONG = "pong";

    final AtomicReference<WebSocket> webSocket = new AtomicReference<>(null);
    final Thread pingThread;

    final String topic;
    final TickerCallback tickerCallback;

    final AtomicReference<Pair<WebSocket, Long>> lastPing = new AtomicReference<>(null);

    public TickerDataStreamer(String topic, TickerCallback tickerCallback) {
        this.topic = topic;
        this.tickerCallback = tickerCallback;

        pingThread = new Thread(() -> {
            while (true) {
                WebSocket socket = webSocket.get();

                if (socket != null) {
                    try {
                        KyKu4_3anpocHuK.ping(socket, "pingu");
                        lastPing.set(Pair.of(socket, System.currentTimeMillis()));

                        try {
                            Thread.sleep(FIVE_SECONDS_IN_MILLIS);
                        } catch (InterruptedException e) {
                            LOGGER.info("Ping thread interrupted", e);
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
                    //First connection
                    reInit(null, -1, null);
                }
            }
        });
    }

    public void start() {
        pingThread.start();
    }

    private WebSocket initWebSocket() throws IOException {
        WebsocketTokenResponse websocketToken = KyKu4_XTTn.getPublicToken();
        LOGGER.info("{}", websocketToken);

        InstanceServer instanceServer = websocketToken.instanceServers().get((int) (websocketToken.instanceServers().size() * Math.random()));
        String streamingUrl = instanceServer.endpoint() + "?token=" + websocketToken.token();
        Request request = new Request.Builder().url(streamingUrl).build();

        return KyKu4_XTTn.newWebSocket(request, this, this);
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
                    KyKu4_3anpocHuK.subscribeOnTicker(newWebSocket, topic);
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
    public void tickerCallback(KucoinEvent<TickerChangeEvent> event) {
        tickerCallback.tickerCallback(event);
    }

    @Override
    public void otherCallback(KucoinEvent event) {
        if (event.type().equals(PONG)) {
            lastPing.set(null);
        } else {
            LOGGER.info("Other message received: type {}", event);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
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
    public void onFailure(WebSocket socket, Throwable t, @Nullable okhttp3.Response response) {
        //try to reinit
        LOGGER.info("Web socket: onFailure; exception {}; response {}", t, response);
        reInit(socket, 1001, t.toString());
    }
}
