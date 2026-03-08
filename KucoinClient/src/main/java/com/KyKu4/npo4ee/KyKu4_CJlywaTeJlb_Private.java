package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.websocket.event.AccountChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.KucoinEvent;
import com.KyKu4.MogeJlb.websocket.event.OrderActivateEvent;
import com.KyKu4.MogeJlb.websocket.event.OrderChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.StopOrderEvent;

//Private
public interface KyKu4_CJlywaTeJlb_Private {
    default void otherCallback(KucoinEvent event) {
    }

    @Deprecated//???
    default void orderActivateCallback(KucoinEvent<OrderActivateEvent> event) {
    }

    /**
     * Account Balance Notice
     * Topic: /account/balance
     * <p>
     * Push frequency: real-time
     * You will receive this message when an account balance changes. The message contains the details of the change.
     * <p>
     * Relation Event:
     * | Type               | Description                       |
     * ----------------------------------------------------------
     * | main.deposit       | Deposit                           |
     * | main.withdraw_hold | Hold withdrawal amount            |
     * | main.withdraw_done | Withdrawal done                   |
     * | main.transfer      | Transfer (Main account)           |
     * | main.other         | Other operations (Main account)   |
     * | trade.hold         | Hold (Trade account)              |
     * | trade.setted       | Settlement (Trade account)        |
     * | trade.transfer     | Transfer (Trade account)          |
     * | trade.other        | Other operations (Trade account)  |
     * | margin.hold        | Hold (Margin account)             |
     * | margin.setted      | Settlement (Margin account)       |
     * | margin.transfer    | Transfer (Margin account)         |
     * | margin.other       | Other operations (Margin account) |
     * | other              | Others                            |
     */
    default void accountChangeCallback(KucoinEvent<AccountChangeEvent> event) {
    }

    /**
     * Private Order Change Events
     * Topic: /spotMarket/tradeOrders
     * <p>
     * Push frequency: real-time
     * This topic will push all change events of your orders.
     * <p>
     * Order Status:
     * “match”: when taker order executes with orders in the order book, the taker order status is “match”;
     * “open”: the order is in the order book;
     * “done”: the order is fully executed successfully;
     * <p>
     * Message Type
     * open
     * when the order enters into the order book;
     */
    default void orderChangeCallback(KucoinEvent<OrderChangeEvent> event) {
    }

    /**
     * Stop Order Event
     * Topic: /spotMarket/advancedOrders
     * <p>
     * Push frequency: real-time
     * Subject: stopOrder
     * <p>
     * When a stop order is received by the system, you will receive a message with "open" type.
     * It means that this order entered the system and waited to be triggered.
     * When a stop order is triggered by current trading price, you will receive a message with "triggered" type.
     * When you cancel a stop order, you will receive a message with "cancel" type.
     */
    default void advancedOrderCallback(KucoinEvent<? extends StopOrderEvent> event) {
    }
}

//TODO: Not implemented:
//Debt Ratio Change
//Position Status Change Event
//Margin Trade Order Enters Event
//Margin Order Update Event
//Margin Order Done Event