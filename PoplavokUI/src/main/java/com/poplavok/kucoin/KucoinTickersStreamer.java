package com.poplavok.kucoin;

import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.TickerChangeEvent;
import com.poplavok.api.kucoin.websocket.TickerCallback;
import com.poplavok.api.kucoin.websocket.TickerDataStreamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KucoinTickersStreamer extends TickerDataStreamer {
    final static Logger LOGGER = LoggerFactory.getLogger(KucoinTickersStreamer.class);

    public static KucoinTickersStreamer createAllTopics() {
        return create(ALL_TICKERS);
    }

    public static KucoinTickersStreamer create(String topic) {
        LOGGER.info("Starting KyKu4_Pugep with topic: {}", topic);
        return new KucoinTickersStreamer(topic, new TickerCallback() {
            @Override
            public void tickerCallback(KucoinEvent<TickerChangeEvent> event) {
                System.out.println(event.toString());
            }
        });
    }

    private KucoinTickersStreamer(String topic, TickerCallback tickerCallback) {
        super(topic, tickerCallback);
    }

    public static void main(String[] args) {
        KucoinTickersStreamer streamer = KucoinTickersStreamer.createAllTopics();
        streamer.start();
    }
}
