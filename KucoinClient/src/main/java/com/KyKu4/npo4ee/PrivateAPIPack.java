package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.retrofit.AccountAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.LoanAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.MarginAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.OrderAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.WebsocketAuthAPIRetrofit;
import com.KyKu4.MogeJlb.retrofit.WithdrawalAPIRetrofit;
import org.immutables.value.Value;

@Value.Immutable
public interface PrivateAPIPack {
    WebsocketAuthAPIRetrofit authAPI();
    AccountAPIRetrofit accountAPI();
    MarginAPIRetrofit marginAPI();
    LoanAPIRetrofit loanAPI();
    WithdrawalAPIRetrofit withdrawalAPI();
    OrderAPIRetrofit orderAPI();
}
