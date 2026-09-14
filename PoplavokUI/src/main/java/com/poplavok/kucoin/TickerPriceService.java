package com.poplavok.kucoin;

import com.poplavok.api.kucoin.websocket.TickerDataStreamer;
import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.TickerChangeEvent;
import com.poplavok.forms.MainForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.net.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-wide service that streams live ticker prices from KuCoin and
 * dispatches them to interested {@link PriceListener}s (e.g. Poplavok forms).
 *
 * <p>A single {@link TickerDataStreamer} (and therefore a single websocket
 * connection) is shared by all listeners. Subscriptions are reference-counted
 * per symbol: the underlying streamer subscribes when the first listener for a
 * symbol registers and unsubscribes once the last listener for that symbol goes
 * away. This lets multiple forms watch the same ticker without subscribing more
 * than once.
 */
public class TickerPriceService {
    final static Logger LOGGER = LoggerFactory.getLogger(TickerPriceService.class);

    private static final TickerPriceService INSTANCE = new TickerPriceService();

    public static TickerPriceService getInstance() {
        return INSTANCE;
    }

    public interface PriceListener {
        void onPrice(String symbol, @Nullable BigDecimal price);
    }

    private final Object lock = new Object();
    private final Map<String, Set<PriceListener>> listenersBySymbol = new ConcurrentHashMap<>();
    // Last price per symbol cache. Only retained while the symbol has active
    // subscribers - dropped once the last subscriber leaves to avoid stale price data.
    private final Map<String, BigDecimal> lastPriceBySymbol = new ConcurrentHashMap<>();
    @Nullable private TickerDataStreamer streamer;

    private TickerPriceService() {
    }

    /** Registers {@code listener} for price updates of {@code symbol}. */
    public void subscribe(String symbol, PriceListener listener) {
        synchronized (lock) {
            TickerDataStreamer activeStreamer = ensureStreamerStarted();

            Set<PriceListener> existing = listenersBySymbol.get(symbol);
            if (existing == null) {
                Set<PriceListener> listeners = ConcurrentHashMap.newKeySet();
                listeners.add(listener);
                listenersBySymbol.put(symbol, listeners);
                activeStreamer.subscribe(symbol);
                LOGGER.info("Subscribed to ticker {}", symbol);
            } else {
                existing.add(listener);
                // Seed the subsequent subscribers with the latest cached price (if any) to
                // avoid a delay before the next stream update arrives.
                BigDecimal cachedPrice = lastPriceBySymbol.get(symbol);
                if (cachedPrice != null) {
                    listener.onPrice(symbol, cachedPrice);
                }
            }
        }
    }

    /** Unregisters a previously registered {@code listener} for {@code symbol}. */
    public void unsubscribe(String symbol, PriceListener listener) {
        synchronized (lock) {
            Set<PriceListener> listeners = listenersBySymbol.get(symbol);
            if (listeners == null) {
                return;
            }
            listeners.remove(listener);

            if (listeners.isEmpty()) {
                listenersBySymbol.remove(symbol);
                lastPriceBySymbol.remove(symbol);
                if (streamer != null) {
                    streamer.unsubscribe(symbol);
                    LOGGER.info("Unsubscribed from ticker {}", symbol);
                }
            }
        }
    }

    private TickerDataStreamer ensureStreamerStarted() {
        if (streamer == null) {
            streamer = new TickerDataStreamer("", this::onTicker, resolveProxy());
            streamer.start();
            LOGGER.info("Ticker price streamer started");
        }
        return streamer;
    }

    /** Stops the underlying streamer and clears all listeners. Called on app shutdown. */
    public void shutdown() {
        synchronized (lock) {
            listenersBySymbol.clear();
            lastPriceBySymbol.clear();
            if (streamer != null) {
                streamer.shutdown();
                streamer = null;
                LOGGER.info("Ticker price streamer shut down");
            }
        }
    }

    @Nullable
    private Proxy resolveProxy() {
        try {
            MainForm mainForm = MainForm.getInstance();
            if (mainForm != null) {
                return mainForm.getApiSettingsDialog().getProxy();
            }
        } catch (Exception e) {
            // No proxy configured / settings unavailable - connect directly.
        }
        return null;
    }

    private void onTicker(KucoinEvent<TickerChangeEvent> event) {
        String symbol = symbolFromTopic(event.topic());
        if (symbol == null) {
            return;
        }

        Set<PriceListener> listeners = listenersBySymbol.get(symbol);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        TickerChangeEvent data = event.data();
        BigDecimal price = data == null ? null : data.price();

        LOGGER.debug("Ticker price update: {} = {}", symbol, price);

        if (price != null) {
            lastPriceBySymbol.put(symbol, price);
        }

        for (PriceListener listener : listeners) {
            try {
                listener.onPrice(symbol, price);
            } catch (Exception e) {
                LOGGER.error("Price listener failed for ticker {}", symbol, e);
            }
        }
    }

    /** Extracts the symbol from a topic such as {@code /market/ticker:BTC-USDT}. */
    @Nullable
    private static String symbolFromTopic(@Nullable String topic) {
        if (topic == null) {
            return null;
        }
        int idx = topic.lastIndexOf(':');
        if (idx < 0 || idx == topic.length() - 1) {
            return null;
        }
        return topic.substring(idx + 1);
    }
}
