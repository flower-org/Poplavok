package com.poplavok.core;

import java.math.BigDecimal;

public interface Operation {
    /** Base currency (e.g. BTC) */
    String base();
    /** Quote currency (e.g. USDT) */
    String quote();
    /** BUY or SELL */
    OperationDirection direction();

    BigDecimal executionPrice();
    BigDecimal commissionPercent();

    /** Quote amount for BUY, Base amount for SELL */
    BigDecimal amountIn(); //
    /** Base amount for BUY, Quote amount for SELL */
    BigDecimal amountOut();
}
