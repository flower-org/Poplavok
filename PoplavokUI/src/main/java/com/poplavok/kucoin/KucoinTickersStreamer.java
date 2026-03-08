package com.poplavok.kucoin;

import com.KyKu4.MogeJlb.websocket.event.KucoinEvent;
import com.KyKu4.MogeJlb.websocket.event.TickerChangeEvent;
import com.KyKu4.npo4ee.TickerCallback;
import com.KyKu4.npo4ee.TickerDataStreamer;
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
