package com.poplavok.api.kucoin.demo;

import com.poplavok.api.kucoin.websocket.Level2DepthStreamer;

public class Depth50Demo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Depth50 Stream for ETH-USDT...");
        
        Level2DepthStreamer streamer = new Level2DepthStreamer(
            "ETH-USDT",
            Level2DepthStreamer.Depth.DEPTH_50,
            event -> {
                if (event.data() != null) {
                    String symbol = event.topic() != null ? event.topic().substring(event.topic().indexOf(":") + 1) : "UNKNOWN";
                    String allBids = event.data().bids() != null ? event.data().bids().toString() : "[]";
                    String allAsks = event.data().asks() != null ? event.data().asks().toString() : "[]";
                    
                    System.out.println("Depth50 Update [" + symbol + "] | Bids: " + allBids + " | Asks: " + allAsks);
                }
            }
        );
        
        streamer.start();

        // Subscribe to another topic after 5 seconds
        Thread.sleep(5000);
        System.out.println("Subscribing to BTC-USDT...");
        streamer.subscribe("BTC-USDT");

        // Unsubscribe from ETH-USDT after another 5 seconds
        Thread.sleep(5000);
        System.out.println("Unsubscribing from ETH-USDT...");
        streamer.unsubscribe("ETH-USDT");

        // Wait for 5 more seconds before shutting down
        Thread.sleep(5000);
        System.out.println("Shutting down streamer...");
        streamer.shutdown();
    }
}
