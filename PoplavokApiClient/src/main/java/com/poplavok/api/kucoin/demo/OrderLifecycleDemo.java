package com.poplavok.api.kucoin.demo;

import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.api.kucoin.auth.BaseKucoinCredentialsProvider;
import com.poplavok.api.kucoin.auth.KucoinCredentialsProvider;
import com.poplavok.api.kucoin.model.request.ImmutableOrderCreateRequest;
import com.poplavok.api.kucoin.model.request.OrderCreateRequest;
import com.poplavok.api.kucoin.model.response.CancelOrderResponse;
import com.poplavok.api.kucoin.model.response.OrderCreateResponse;
import com.poplavok.api.kucoin.model.response.OrderResponse;
import com.poplavok.api.kucoin.model.response.Pagination;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A demonstration of the full order lifecycle (Create -> Check Status -> Cancel -> Check Final Status)
 * specifically using Isolated Margin Trading.
 */
public class OrderLifecycleDemo {
    public static void main(String[] args) {
        // NOTE: Replace these with your actual KuCoin API credentials.
        String apiKey = System.getenv("KUCOIN_API_KEY");
        String secret = System.getenv("KUCOIN_SECRET");
        String passPhrase = System.getenv("KUCOIN_PASSPHRASE");

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Warning: KUCOIN_API_KEY environment variable not set. Running with dummy credentials. API calls will fail.");
            apiKey = "6a48c72be0ac4d00015f81f5";
            secret = "9895cbe1-5b09-40d0-b738-6ad60e13732a";
            passPhrase = "diFHWy@B]ZUy@+FL'!\\EQx.w3L.EL";
        }

        final String finalApiKey = apiKey;
        final String finalSecret = secret;
        final String finalPassPhrase = passPhrase;

        KucoinCredentialsProvider provider = new BaseKucoinCredentialsProvider() {
            @Override public String getApiKey() { return finalApiKey; }
            @Override public String getSecret() { return finalSecret; }
            @Override public String getPassPhrase() { return finalPassPhrase; }
        };

        KucoinApiClient client = new KucoinApiClient(provider);

        try {
            System.out.println("=== 0. Reading All Active Orders ===");
            Pagination<OrderResponse> activeOrders = client.getOrderList("MARGIN_ISOLATED_TRADE", "active", null, null);
            System.out.println("-> Active Orders Count: " + (activeOrders.items() != null ? activeOrders.items().size() : 0));
            System.out.println();
/*
            System.out.println("=== 1. Creating an Isolated Margin Buy Order ===");
            // Creating a Limit Buy order at a deliberately low price to avoid instant fill
            OrderCreateRequest buyRequest = ImmutableOrderCreateRequest.builder()
                    .clientOid(UUID.randomUUID().toString())
                    .side("buy")
                    .symbol("BTC-USDT")
                    .type("limit")
                    .price(new BigDecimal("30000.00")) // $30k for BTC
                    .size(new BigDecimal("0.001"))
                    .isIsolated(true)
                    .build();

            System.out.println("Sending Request: " + buyRequest);
            OrderCreateResponse buyResponse = client.createMarginOrder(buyRequest);
            String orderId = buyResponse.orderId();
            if (orderId == null) {
                throw new IllegalStateException("Order ID returned from exchange is null");
            }
            System.out.println("-> Created Buy Order ID: " + orderId);

            System.out.println("\n=== 2. Checking Intermediate Order Status ===");
            Thread.sleep(1000); // Give the exchange a moment to register the order
            OrderResponse orderStatus = client.getOrder(orderId);
            System.out.println("-> Order is Active: " + orderStatus.isActive());
            System.out.println("-> Executed Size: " + orderStatus.dealSize());
            System.out.println("-> Executed Funds: " + orderStatus.dealFunds());

            System.out.println("\n=== 3. Cancelling the Order ===");
            // We cancel the order, keeping any partial execution that might have happened
            CancelOrderResponse cancelResponse = client.cancelOrder(orderId);
            System.out.println("-> Cancelled Order IDs: " + cancelResponse.cancelledOrderIds());

            System.out.println("\n=== 4. Verifying Final Outcome ===");
            Thread.sleep(1000); // Wait for cancellation to process fully
            OrderResponse finalStatus = client.getOrder(orderId);
            System.out.println("-> Final Order Active Status: " + finalStatus.isActive());
            System.out.println("-> Final Executed Size (partial if any): " + finalStatus.dealSize());
*/
            System.out.println("\n--- Demo Completed Successfully ---");

        } catch (Exception e) {
            System.err.println("\n[Demo Interrupted] " + e.getMessage());
            // It is expected to fail if run with dummy credentials
        }
    }
}
