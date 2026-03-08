package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.retrofit.KucoinAPIRetrofit;
import org.immutables.value.Value;

@Value.Immutable
public interface KucoinAPIPack {
    KucoinAPIRetrofit kucoinAPI();
}
