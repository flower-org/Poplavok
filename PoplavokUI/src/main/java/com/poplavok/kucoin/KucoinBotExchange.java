package com.poplavok.kucoin;

import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.api.kucoin.exception.KucoinApiException;
import com.poplavok.api.kucoin.model.request.ImmutableOrderCreateRequest;
import com.poplavok.api.kucoin.model.response.OrderResponse;
import com.poplavok.api.kucoin.model.response.SymbolResponse;
import com.poplavok.data.bot.BotExchange;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * {@link BotExchange} on KuCoin's isolated margin account: HF margin GTC limit orders with auto-borrow and auto-repay
 * off, so the debt stays as it is and the QUOTE from sells stays in the account.
 * <p>
 * Orders are rounded to the symbol's increments in the bot's favour (sizes down, sell prices up, buy prices down) and
 * refused with {@link OrderTooSmallException} below its minimum size or value. Every order gets a client order id; if
 * placing it fails without a clear answer from KuCoin (network error, 5xx), the order is looked up by that id, so a
 * timed-out order that KuCoin did accept isn't lost. KuCoin rejections are thrown as {@link KucoinApiException}.
 * Fees count only if paid in QUOTE (e.g. not when paid in KCS).
 */
public class KucoinBotExchange implements BotExchange {
    static final String TRADE_TYPE = "MARGIN_ISOLATED_TRADE";
    /** Exchange name bots bind to. */
    public static final String NAME = "KuCoin isolated margin";

    /** Where price updates come from. */
    public interface PriceSource {
        Subscription subscribe(String symbol, PriceListener listener) throws IOException;
    }

    private final KucoinApiClient client;
    private final PriceSource prices;
    private final Supplier<String> clientOids;
    private final Map<String, SymbolResponse> rules = new ConcurrentHashMap<>();

    public KucoinBotExchange(KucoinApiClient client, PriceSource prices) {
        this(client, prices, () -> UUID.randomUUID().toString());
    }

    KucoinBotExchange(KucoinApiClient client, PriceSource prices, Supplier<String> clientOids) {
        this.client = client;
        this.prices = prices;
        this.clientOids = clientOids;
    }

    /** Prices from the app's ticker stream. */
    public static PriceSource tickerPrices() {
        return (symbol, listener) -> {
            TickerPriceService.PriceListener tickerListener = (ticker, price) -> {
                if (price != null) {
                    listener.onPrice(price);
                }
            };
            TickerPriceService.getInstance().subscribe(symbol, tickerListener);
            AtomicBoolean closed = new AtomicBoolean();
            return () -> {
                if (closed.compareAndSet(false, true)) {
                    TickerPriceService.getInstance().unsubscribe(symbol, tickerListener);
                }
            };
        };
    }

    @Override
    public Order placeLimitOrder(String symbol, Side side, BigDecimal price, BigDecimal size) throws IOException {
        SymbolResponse symbolRules = rules(symbol);
        BigDecimal roundedSize = round(size, symbolRules.baseIncrement(), RoundingMode.FLOOR);
        BigDecimal roundedPrice = round(price, symbolRules.priceIncrement(),
                side == Side.SELL ? RoundingMode.CEILING : RoundingMode.FLOOR);
        if (roundedPrice.signum() <= 0) {
            throw new IOException("price " + price.toPlainString() + " rounds to 0");
        }
        BigDecimal minSize = symbolRules.baseMinSize();
        BigDecimal minFunds = symbolRules.minFunds();
        if (roundedSize.signum() <= 0 || (minSize != null && roundedSize.compareTo(minSize) < 0)) {
            throw new OrderTooSmallException("size " + roundedSize.toPlainString() + " is below the minimum "
                    + (minSize == null ? "0" : minSize.toPlainString()));
        }
        if (minFunds != null && roundedSize.multiply(roundedPrice).compareTo(minFunds) < 0) {
            throw new OrderTooSmallException("value " + roundedSize.multiply(roundedPrice).toPlainString()
                    + " is below the minimum " + minFunds.toPlainString());
        }
        String clientOid = clientOids.get();
        String orderId;
        try {
            orderId = client.createMarginOrder(ImmutableOrderCreateRequest.builder()
                    .clientOid(clientOid)
                    .symbol(symbol)
                    .side(side == Side.SELL ? "sell" : "buy")
                    .type("limit")
                    .price(roundedPrice)
                    .size(roundedSize)
                    .timeInForce("GTC")
                    .isIsolated(true)
                    .autoBorrow(false)
                    .autoRepay(false)
                    .build()).orderId();
        } catch (KucoinApiException e) {
            if (!isServerError(e)) {
                throw e;
            }
            return recover(symbol, clientOid, e);
        } catch (IOException e) {
            return recover(symbol, clientOid, e);
        }
        if (orderId == null) {
            return recover(symbol, clientOid, new IOException("KuCoin returned no order id"));
        }
        return new Order(orderId, side, roundedPrice, roundedSize, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                OrderStatus.OPEN);
    }

    /** After placing failed without a clear answer: the order if KuCoin has it, else {@code failure}. */
    private Order recover(String symbol, String clientOid, Exception failure) throws IOException {
        Exception lookupFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return toOrder(symbol, client.getMarginOrderByClientOid(clientOid, symbol));
            } catch (KucoinApiException e) {
                if (!isServerError(e)) {
                    // KuCoin doesn't know the order: it wasn't placed
                    if (failure instanceof IOException io) {
                        throw io;
                    }
                    throw (KucoinApiException) failure;
                }
                lookupFailure = e;
            } catch (IOException e) {
                lookupFailure = e;
            }
        }
        IOException unknown = new IOException("order " + clientOid + " may have been placed, couldn't check: "
                + failure.getMessage(), failure);
        if (lookupFailure != null) {
            unknown.addSuppressed(lookupFailure);
        }
        throw unknown;
    }

    @Override
    public Order getOrder(String symbol, String orderId) throws IOException {
        return toOrder(symbol, client.getMarginOrder(orderId, symbol));
    }

    @Override
    public void cancelOrder(String symbol, String orderId) throws IOException {
        client.cancelMarginOrder(orderId, symbol);
    }

    @Override
    public String exchangeName() {
        return NAME;
    }

    @Override
    public Subscription subscribePrice(String symbol, PriceListener listener) throws IOException {
        return prices.subscribe(symbol, listener);
    }

    Order toOrder(String symbol, OrderResponse response) throws IOException {
        String id = response.id();
        BigDecimal price = response.price();
        BigDecimal size = response.size();
        Boolean active = response.active() != null ? response.active() : response.isActive();
        if (id == null || price == null || size == null || active == null || response.side() == null) {
            throw new IOException("incomplete order from KuCoin: " + response);
        }
        BigDecimal filled = zeroIfNull(response.dealSize());
        OrderStatus status = active ? OrderStatus.OPEN
                : filled.compareTo(size) >= 0 ? OrderStatus.FILLED : OrderStatus.CANCELLED;
        BigDecimal fee = quoteCurrency(symbol).equals(response.feeCurrency()) ? zeroIfNull(response.fee())
                : BigDecimal.ZERO;
        return new Order(id, "sell".equals(response.side()) ? Side.SELL : Side.BUY, price, size, filled,
                zeroIfNull(response.dealFunds()), fee, status);
    }

    private SymbolResponse rules(String symbol) throws IOException {
        SymbolResponse cached = rules.get(symbol);
        if (cached == null) {
            cached = client.getSymbol(symbol);
            rules.put(symbol, cached);
        }
        return cached;
    }

    private String quoteCurrency(String symbol) throws IOException {
        String quote = rules(symbol).quoteCurrency();
        return quote != null ? quote : symbol.substring(symbol.indexOf('-') + 1);
    }

    static BigDecimal round(BigDecimal value, @Nullable BigDecimal increment, RoundingMode mode) {
        if (increment == null || increment.signum() <= 0) {
            return value;
        }
        return value.divide(increment, 0, mode).multiply(increment);
    }

    /** HTTP 5xx: KuCoin may or may not have acted. */
    static boolean isServerError(KucoinApiException e) {
        String code = e.getCode();
        return code != null && code.length() == 3 && code.startsWith("5");
    }

    private static BigDecimal zeroIfNull(@Nullable BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
