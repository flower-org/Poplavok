package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.retrofit.CurrencyAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.KucoinAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.SymbolAPIRetrofit;
import org.immutables.value.Value;

@Value.Immutable
public interface PublicAPIPack {
    CurrencyAPIRetrofit currencyAPI();
    SymbolAPIRetrofit symbolAPI();
    KucoinAPIRetrofit kucoinAPI();
}
