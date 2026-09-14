package com.poplavok.api.kucoin.websocket;

import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.TickerChangeEvent;
import okhttp3.WebSocket;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TickerDataStreamerTest {

    private static final String TOPIC = "BTC-USDT";

    private TickerCallback callback;
    private TickerDataStreamer streamer;

    @BeforeEach
    void setUp() {
        callback = mock(TickerCallback.class);
        streamer = new TickerDataStreamer(TOPIC, callback);
    }

    @AfterEach
    void tearDown() {
        streamer.shutdown();
    }

    // ----- construction / topic set -----

    @Test
    void constructorRegistersInitialTopic() {
        assertTrue(streamer.topics.contains(TOPIC));
        assertEquals(1, streamer.topics.size());
    }

    @Test
    void constructorIgnoresNullOrEmptyTopic() {
        TickerDataStreamer nullTopic = new TickerDataStreamer(null, callback);
        TickerDataStreamer emptyTopic = new TickerDataStreamer("", callback);
        try {
            assertTrue(nullTopic.topics.isEmpty());
            assertTrue(emptyTopic.topics.isEmpty());
        } finally {
            nullTopic.shutdown();
            emptyTopic.shutdown();
        }
    }

    // ----- subscribe / unsubscribe -----

    @Test
    void subscribeWithoutConnectionJustTracksTopic() {
        streamer.subscribe("ETH-USDT");
        assertTrue(streamer.topics.contains("ETH-USDT"));
    }

    @Test
    void subscribeWhenConnectedSendsSubscribeFrame() {
        WebSocket socket = mock(WebSocket.class);
        streamer.webSocket.set(socket);

        streamer.subscribe("ETH-USDT");

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(socket).send(sent.capture());
        assertTrue(sent.getValue().contains("\"type\":\"subscribe\""));
        assertTrue(sent.getValue().contains("/market/ticker:ETH-USDT"));
    }

    @Test
    void subscribeDuplicateTopicDoesNotResend() {
        WebSocket socket = mock(WebSocket.class);
        streamer.webSocket.set(socket);

        streamer.subscribe("ETH-USDT");
        streamer.subscribe("ETH-USDT");

        verify(socket, times(1)).send(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void unsubscribeWhenConnectedSendsUnsubscribeFrameAndRemovesTopic() {
        WebSocket socket = mock(WebSocket.class);
        streamer.webSocket.set(socket);

        streamer.unsubscribe(TOPIC);

        assertFalse(streamer.topics.contains(TOPIC));
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(socket).send(sent.capture());
        assertTrue(sent.getValue().contains("\"type\":\"unsubscribe\""));
        assertTrue(sent.getValue().contains("/market/ticker:" + TOPIC));
    }

    @Test
    void unsubscribeUnknownTopicDoesNothing() {
        WebSocket socket = mock(WebSocket.class);
        streamer.webSocket.set(socket);

        streamer.unsubscribe("NOPE-USDT");

        verify(socket, never()).send(org.mockito.ArgumentMatchers.anyString());
    }

    // ----- pong / ping matching -----

    @Test
    void matchingPongClearsPendingPing() {
        WebSocket socket = mock(WebSocket.class);
        streamer.lastPing.set(Pair.of(socket, "ping-5"));

        streamer.onMessage(socket, "{\"id\":\"ping-5\",\"type\":\"pong\"}");

        assertNull(streamer.lastPing.get());
    }

    @Test
    void stalePongDoesNotClearFreshPing() {
        WebSocket socket = mock(WebSocket.class);
        Pair<WebSocket, String> fresh = Pair.of(socket, "ping-6");
        streamer.lastPing.set(fresh);

        // A late pong from an earlier cycle must not clear the current pending ping.
        streamer.onMessage(socket, "{\"id\":\"ping-5\",\"type\":\"pong\"}");

        assertSame(fresh, streamer.lastPing.get());
    }

    // ----- message dispatch -----

    @Test
    void tickerMessageIsDispatchedToCallback() {
        String json = "{\"type\":\"message\","
                + "\"topic\":\"/market/ticker:BTC-USDT\","
                + "\"subject\":\"trade.ticker\","
                + "\"data\":{\"price\":\"50000\",\"bestBid\":\"49999\",\"bestAsk\":\"50001\",\"size\":\"0.1\",\"sequence\":\"123\"}}";

        streamer.onMessage(mock(WebSocket.class), json);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<KucoinEvent<TickerChangeEvent>> captor =
                ArgumentCaptor.forClass((Class) KucoinEvent.class);
        verify(callback).tickerCallback(captor.capture());

        KucoinEvent<TickerChangeEvent> event = captor.getValue();
        assertEquals("message", event.type());
        assertEquals("/market/ticker:BTC-USDT", event.topic());
        TickerChangeEvent data = event.data();
        assertEquals(new BigDecimal("50000"), data.price());
        assertEquals(new BigDecimal("49999"), data.bestBid());
        assertEquals("123", data.sequence());
    }

    @Test
    void nonMessageEventDoesNotInvokeCallback() {
        streamer.onMessage(mock(WebSocket.class), "{\"type\":\"welcome\",\"id\":\"abc\"}");
        verify(callback, never()).tickerCallback(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void malformedJsonIsSwallowed() {
        // Should not throw; parse failure is logged and ignored.
        streamer.onMessage(mock(WebSocket.class), "not-json");
        verify(callback, never()).tickerCallback(org.mockito.ArgumentMatchers.any());
    }

    // ----- shutdown behaviour -----

    @Test
    void shutdownSetsFlagAndPreventsReconnectOnFailure() {
        streamer.shutdown();
        assertTrue(streamer.shuttingDown.get());

        WebSocket socket = mock(WebSocket.class);
        streamer.webSocket.set(socket);

        // While shutting down, a failure must not trigger a reconnect (which would hit the network).
        streamer.onFailure(socket, new RuntimeException("boom"), null);

        assertFalse(streamer.reconnecting.get());
    }

    @Test
    void shutdownClosesActiveSocket() {
        WebSocket socket = mock(WebSocket.class);
        streamer.webSocket.set(socket);

        streamer.shutdown();

        verify(socket).close(1000, "Shutting down");
        assertNull(streamer.webSocket.get());
    }
}
