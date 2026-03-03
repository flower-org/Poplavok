package com.poplavok.core;

import java.math.BigDecimal;

public interface Poplavok {
    /** Base currency (e.g. BTC) */
    String base();
    /** Quote currency (e.g. USDT) */
    String quote();
    /** LONG or SHORT */
    PoplavokDirection direction();

    /** Step coefficient */
    BigDecimal step();
    /** Mul coefficient */
    BigDecimal mul();
    /** Trading commission on a pair */
    BigDecimal commission();

    Level entry();
}
