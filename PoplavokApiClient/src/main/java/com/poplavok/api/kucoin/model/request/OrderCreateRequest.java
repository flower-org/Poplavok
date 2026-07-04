package com.poplavok.api.kucoin.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableOrderCreateRequest.class)
@JsonDeserialize(as = ImmutableOrderCreateRequest.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface OrderCreateRequest {
    
    /** Unique order id created by users to identify their orders, e.g. UUID */
    String clientOid();

    /** buy or sell */
    String side();

    /** e.g. BTC-USDT */
    String symbol();

    /** limit, market, stop_limit or stop_market */
    @Nullable
    String type();

    /** remark for the order, length cannot exceed 100 utf8 characters */
    @Nullable
    String remark();

    /** self trade prevention , CN, CO, CB or DC */
    @Nullable
    String stp();

    /** marginModel: cross or isolated */
    @Nullable
    String marginModel();

    /** tradeType: TRADE, MARGIN_TRADE, MARGIN_ISOLATED_TRADE */
    @Nullable
    String tradeType();

    /** price per base currency */
    @Nullable
    BigDecimal price();

    /** amount of base currency to buy or sell */
    @Nullable
    BigDecimal size();

    /** time in force: GTC, GTT, IOC, FOK */
    @Nullable
    String timeInForce();

    /** cancel after n seconds, requires timeInForce to be GTT */
    @Nullable
    Long cancelAfter();

    /** post only flag */
    @Nullable
    Boolean postOnly();

    /** hidden order flag */
    @Nullable
    Boolean hidden();

    /** iceberg order flag */
    @Nullable
    Boolean iceberg();

    /** maximum visible size of an iceberg order */
    @Nullable
    BigDecimal visibleSize();
    
    /** The amount of quote currency to spend. For market orders. */
    @Nullable
    BigDecimal funds();

    /** auto borrow flag for margin orders */
    @Nullable
    Boolean autoBorrow();
}
