package com.poplavok.core;

import java.math.BigDecimal;
import java.util.List;

public interface Level {

    BigDecimal levelPrice();
    BigDecimal levelAmount();

    BigDecimal borrowed();

    List<Operation> operations();
    boolean isClosed();
}
