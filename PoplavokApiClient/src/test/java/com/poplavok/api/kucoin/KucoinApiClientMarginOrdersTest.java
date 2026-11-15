package com.poplavok.api.kucoin;

import com.poplavok.api.kucoin.model.response.MarginOrdersPage;
import com.poplavok.api.kucoin.model.response.OrderResponse;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KucoinApiClientMarginOrdersTest {
    static final String ORDER = "{\"id\":\"671667306afcdb000723107f\",\"clientOid\":\"5c52e11203aa677f33e493fb\","
            + "\"symbol\":\"BTC-USDT\",\"opType\":\"DEAL\",\"type\":\"limit\",\"side\":\"buy\",\"price\":\"50000\","
            + "\"size\":\"0.00001\",\"funds\":\"0.5\",\"dealSize\":\"0.000004\",\"dealFunds\":\"0.2\","
            + "\"remainSize\":\"0.000006\",\"remainFunds\":\"0.3\",\"cancelledSize\":\"0\",\"cancelledFunds\":\"0\","
            + "\"fee\":\"0\",\"feeCurrency\":\"USDT\",\"stp\":null,\"stop\":null,\"stopTriggered\":false,"
            + "\"stopPrice\":\"0\",\"timeInForce\":\"GTC\",\"postOnly\":false,\"hidden\":false,\"iceberg\":false,"
            + "\"visibleSize\":\"0\",\"cancelAfter\":0,\"channel\":\"API\",\"remark\":null,\"tags\":null,"
            + "\"cancelExist\":false,\"tradeType\":\"MARGIN_ISOLATED_TRADE\",\"inOrderBook\":true,\"active\":true,"
            + "\"tax\":\"0\",\"createdAt\":1729521456248,\"lastUpdatedAt\":1729651011877,\"cancelReason\":0}";

    final List<Request> requests = new ArrayList<>();

    KucoinApiClient client(String data) {
        Interceptor answer = chain -> {
            requests.add(chain.request());
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("OK").body(ResponseBody.create("{\"code\":\"200000\",\"data\":" + data + "}",
                            MediaType.parse("application/json"))).build();
        };
        return new KucoinApiClient(null, null, answer);
    }

    @Test
    void getsMarginOrderById() throws Exception {
        OrderResponse order = client(ORDER).getMarginOrder("671667306afcdb000723107f", "BTC-USDT");
        assertEquals("https://api.kucoin.com/api/v3/hf/margin/orders/671667306afcdb000723107f?symbol=BTC-USDT",
                requests.get(0).url().toString());
        assertEquals("GET", requests.get(0).method());
        assertEquals(Boolean.TRUE, order.active());
        assertEquals(Boolean.TRUE, order.inOrderBook());
        assertEquals(0, new BigDecimal("0.000004").compareTo(order.dealSize()));
        assertEquals(0, new BigDecimal("0.000006").compareTo(order.remainSize()));
        assertEquals(1729651011877L, order.lastUpdatedAt().getTime());
        assertEquals("MARGIN_ISOLATED_TRADE", order.tradeType());
    }

    @Test
    void getsMarginOrderByClientOid() throws Exception {
        OrderResponse order = client(ORDER).getMarginOrderByClientOid("5c52e11203aa677f33e493fb", "BTC-USDT");
        assertEquals("https://api.kucoin.com/api/v3/hf/margin/orders/client-order/5c52e11203aa677f33e493fb"
                + "?symbol=BTC-USDT", requests.get(0).url().toString());
        assertEquals("5c52e11203aa677f33e493fb", order.clientOid());
    }

    @Test
    void getsMarginActiveOrders() throws Exception {
        List<OrderResponse> orders = client("[" + ORDER + "]").getMarginActiveOrders("BTC-USDT",
                "MARGIN_ISOLATED_TRADE");
        assertEquals("https://api.kucoin.com/api/v3/hf/margin/orders/active?symbol=BTC-USDT"
                + "&tradeType=MARGIN_ISOLATED_TRADE", requests.get(0).url().toString());
        assertEquals(1, orders.size());
    }

    @Test
    void getsMarginDoneOrders() throws Exception {
        String done = ORDER.replace("\"active\":true", "\"active\":false")
                .replace("\"inOrderBook\":true", "\"inOrderBook\":false");
        MarginOrdersPage page = client("{\"lastId\":136112949351,\"items\":[" + done + "]}")
                .getMarginDoneOrders("BTC-USDT", "MARGIN_ISOLATED_TRADE", "sell", "limit", 254062248624417L, 50,
                        1728663338000L, 1728692138000L);
        assertEquals("https://api.kucoin.com/api/v3/hf/margin/orders/done?symbol=BTC-USDT"
                        + "&tradeType=MARGIN_ISOLATED_TRADE&side=sell&type=limit&lastId=254062248624417&limit=50"
                        + "&startAt=1728663338000&endAt=1728692138000", requests.get(0).url().toString());
        assertEquals(136112949351L, page.lastId());
        assertFalse(page.items().get(0).active());
    }

    @Test
    void optionalDoneOrderParamsAreOmitted() throws Exception {
        client("{\"items\":[]}").getMarginDoneOrders("BTC-USDT", "MARGIN_ISOLATED_TRADE", null, null, null, null,
                null, null);
        assertEquals("https://api.kucoin.com/api/v3/hf/margin/orders/done?symbol=BTC-USDT"
                + "&tradeType=MARGIN_ISOLATED_TRADE", requests.get(0).url().toString());
        assertTrue(requests.get(0).url().querySize() == 2);
    }
}
