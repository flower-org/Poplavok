package com.poplavok.kucoin;

import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.api.kucoin.exception.KucoinApiException;
import com.poplavok.data.bot.BotExchange;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KucoinBotExchangeTest {
    static final String SYMBOL = "{\"symbol\":\"HBAR-USDT\",\"baseCurrency\":\"HBAR\",\"quoteCurrency\":\"USDT\","
            + "\"baseMinSize\":\"0.1\",\"baseIncrement\":\"0.0001\",\"priceIncrement\":\"0.00001\","
            + "\"minFunds\":\"0.1\",\"enableTrading\":true}";
    static final String ORDER = "{\"id\":\"o1\",\"clientOid\":\"cid\",\"symbol\":\"HBAR-USDT\",\"type\":\"limit\","
            + "\"side\":\"sell\",\"price\":\"0.12346\",\"size\":\"100\",\"dealSize\":\"40\",\"dealFunds\":\"4.9384\","
            + "\"fee\":\"0.0049384\",\"feeCurrency\":\"USDT\",\"active\":true}";
    static final IOException TIMEOUT = new IOException("timeout");

    final List<Request> requests = new ArrayList<>();
    final List<String> bodies = new ArrayList<>();
    /** Answers a request with KuCoin's {"code","data"} JSON, or throws {@link #TIMEOUT} if it returns null. */
    Function<Request, String> answer = request -> null;

    KucoinBotExchange exchange() {
        Interceptor interceptor = chain -> {
            Request request = chain.request();
            requests.add(request);
            Buffer buffer = new Buffer();
            if (request.body() != null) {
                request.body().writeTo(buffer);
            }
            bodies.add(buffer.readUtf8());
            String json = request.url().encodedPath().startsWith("/api/v2/symbols/") ? ok(SYMBOL)
                    : answer.apply(request);
            if (json == null) {
                throw TIMEOUT;
            }
            int code = json.startsWith("HTTP ") ? Integer.parseInt(json.substring(5, 8)) : 200;
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("x")
                    .body(ResponseBody.create(code == 200 ? json : "{\"msg\":\"down\"}",
                            MediaType.parse("application/json"))).build();
        };
        return new KucoinBotExchange(new KucoinApiClient(null, null, interceptor),
                (symbol, listener) -> () -> { }, () -> "cid");
    }

    static String ok(String data) {
        return "{\"code\":\"200000\",\"data\":" + data + "}";
    }

    List<String> paths() {
        return requests.stream().map(r -> r.method() + " " + r.url().encodedPath()).toList();
    }

    @Test
    void placesIsolatedLimitOrderRoundedInTheBotsFavour() throws Exception {
        answer = request -> ok("{\"orderId\":\"o1\"}");
        KucoinBotExchange exchange = exchange();
        BotExchange.Order sell = exchange.placeLimitOrder("HBAR-USDT", BotExchange.Side.SELL,
                new BigDecimal("0.123451"), new BigDecimal("100.12349"));
        assertEquals(new BotExchange.Order("o1", BotExchange.Side.SELL, new BigDecimal("0.12346"),
                new BigDecimal("100.1234"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BotExchange.OrderStatus.OPEN), sell, "size down, sell price up");
        assertEquals(List.of("GET /api/v2/symbols/HBAR-USDT", "POST /api/v3/hf/margin/order"), paths());
        String body = bodies.get(1);
        for (String field : List.of("\"clientOid\":\"cid\"", "\"side\":\"sell\"", "\"symbol\":\"HBAR-USDT\"",
                "\"type\":\"limit\"", "\"price\":0.12346", "\"size\":100.1234", "\"timeInForce\":\"GTC\"",
                "\"isIsolated\":true", "\"autoBorrow\":false", "\"autoRepay\":false")) {
            assertTrue(body.contains(field), field + " in " + body);
        }

        BotExchange.Order buy = exchange.placeLimitOrder("HBAR-USDT", BotExchange.Side.BUY,
                new BigDecimal("0.123459"), new BigDecimal("100"));
        assertEquals(new BigDecimal("0.12345"), buy.price(), "buy price down");
        assertEquals(3, requests.size(), "symbol rules are cached");
    }

    @Test
    void tooSmallOrdersAreRefusedWithoutCallingKuCoin() {
        answer = request -> ok("{\"orderId\":\"o1\"}");
        KucoinBotExchange exchange = exchange();
        assertThrows(BotExchange.OrderTooSmallException.class, () -> exchange.placeLimitOrder("HBAR-USDT",
                BotExchange.Side.SELL, new BigDecimal("0.12"), new BigDecimal("0.09999")), "below min size");
        assertThrows(BotExchange.OrderTooSmallException.class, () -> exchange.placeLimitOrder("HBAR-USDT",
                BotExchange.Side.SELL, new BigDecimal("0.12"), new BigDecimal("0.5")), "below min funds");
        assertEquals(List.of("GET /api/v2/symbols/HBAR-USDT"), paths());
    }

    @Test
    void timedOutOrderThatKuCoinAcceptedIsFoundByClientOid() throws Exception {
        answer = request -> request.method().equals("POST") ? null : ok(ORDER);
        BotExchange.Order order = exchange().placeLimitOrder("HBAR-USDT", BotExchange.Side.SELL,
                new BigDecimal("0.12346"), new BigDecimal("100"));
        assertEquals("o1", order.orderId());
        assertEquals(BotExchange.OrderStatus.OPEN, order.status());
        assertEquals("GET /api/v3/hf/margin/orders/client-order/cid", paths().get(2));
    }

    @Test
    void timedOutOrderThatKuCoinDoesNotKnowFailsWithTheTimeout() {
        answer = request -> request.method().equals("POST") ? null
                : "{\"code\":\"400100\",\"msg\":\"order not exist\"}";
        IOException e = assertThrows(IOException.class, () -> exchange().placeLimitOrder("HBAR-USDT",
                BotExchange.Side.SELL, new BigDecimal("0.12346"), new BigDecimal("100")));
        assertSame(TIMEOUT, e);
    }

    @Test
    void unknownOutcomeIsReportedWhenTheLookupFailsToo() {
        answer = request -> null;
        IOException e = assertThrows(IOException.class, () -> exchange().placeLimitOrder("HBAR-USDT",
                BotExchange.Side.SELL, new BigDecimal("0.12346"), new BigDecimal("100")));
        assertTrue(e.getMessage().startsWith("order cid may have been placed"), e.getMessage());
        assertEquals(4, requests.size(), "symbol, place, 2 lookups");
    }

    @Test
    void serverErrorIsCheckedByClientOidButRejectionIsNot() {
        answer = request -> request.method().equals("POST") ? "HTTP 503" : ok(ORDER);
        try {
            assertEquals("o1", exchange().placeLimitOrder("HBAR-USDT", BotExchange.Side.SELL,
                    new BigDecimal("0.12346"), new BigDecimal("100")).orderId());
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        requests.clear();
        answer = request -> "{\"code\":\"200004\",\"msg\":\"Balance insufficient\"}";
        KucoinApiException e = assertThrows(KucoinApiException.class, () -> exchange().placeLimitOrder(
                "HBAR-USDT", BotExchange.Side.SELL, new BigDecimal("0.12346"), new BigDecimal("100")));
        assertEquals("200004", e.getCode());
        assertEquals(2, requests.size(), "no lookup after a rejection");
    }

    @Test
    void ordersAreReadWithTheirStatusAndQuoteFees() throws Exception {
        answer = request -> ok(ORDER);
        KucoinBotExchange exchange = exchange();
        BotExchange.Order open = exchange.getOrder("HBAR-USDT", "o1");
        assertEquals(new BotExchange.Order("o1", BotExchange.Side.SELL, new BigDecimal("0.12346"),
                new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("4.9384"), new BigDecimal("0.0049384"),
                BotExchange.OrderStatus.OPEN), open);
        assertEquals("GET /api/v3/hf/margin/orders/o1", paths().get(0));
        assertEquals("symbol=HBAR-USDT", requests.get(0).url().encodedQuery());

        answer = request -> ok(ORDER.replace("\"active\":true", "\"active\":false")
                .replace("\"dealSize\":\"40\"", "\"dealSize\":\"100\""));
        assertEquals(BotExchange.OrderStatus.FILLED, exchange.getOrder("HBAR-USDT", "o1").status());

        answer = request -> ok(ORDER.replace("\"active\":true", "\"active\":false")
                .replace("\"feeCurrency\":\"USDT\"", "\"feeCurrency\":\"KCS\""));
        BotExchange.Order cancelled = exchange.getOrder("HBAR-USDT", "o1");
        assertEquals(BotExchange.OrderStatus.CANCELLED, cancelled.status());
        assertEquals(BigDecimal.ZERO, cancelled.fee(), "fee not paid in QUOTE");
    }

    @Test
    void cancelsHfMarginOrder() throws Exception {
        answer = request -> ok("{\"orderId\":\"o1\"}");
        exchange().cancelOrder("HBAR-USDT", "o1");
        assertEquals(List.of("DELETE /api/v3/hf/margin/orders/o1"), paths());
        assertEquals("symbol=HBAR-USDT", requests.get(0).url().encodedQuery());
    }
}
