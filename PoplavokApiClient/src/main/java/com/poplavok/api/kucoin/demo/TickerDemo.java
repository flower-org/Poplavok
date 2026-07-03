package com.poplavok.api.kucoin.demo;

import com.poplavok.api.kucoin.websocket.TickerDataStreamer;

public class TickerDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Ticker Stream for BTC-USDT...");
        
        TickerDataStreamer streamer = new TickerDataStreamer(
            "BTC-USDT",
            event -> {
                if (event.data() != null) {
                    String symbol = event.topic() != null ? event.topic().replace("/market/ticker:", "") : "UNKNOWN";
                    System.out.println("Ticker Update [" + symbol + "] | Price: " + event.data().price() + " | Best Bid: " + event.data().bestBid() + " | Best Ask: " + event.data().bestAsk());
                }
            }
        );
        
        streamer.start();

        // Subscribe to another topic after 5 seconds
        Thread.sleep(5000);
        System.out.println("Subscribing to ETH-USDT...");
        streamer.subscribe("ETH-USDT");

        // Unsubscribe from BTC-USDT after another 5 seconds
        Thread.sleep(5000);
        System.out.println("Unsubscribing from BTC-USDT...");
        streamer.unsubscribe("BTC-USDT");

        // Wait for 5 more seconds before shutting down
        Thread.sleep(5000);
        System.out.println("Shutting down streamer...");
        streamer.shutdown();
    }
}
