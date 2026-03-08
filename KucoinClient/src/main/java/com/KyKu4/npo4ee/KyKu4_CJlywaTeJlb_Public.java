package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.websocket.event.KucoinEvent;
import com.KyKu4.MogeJlb.websocket.event.Level2ChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.Level2Event;
import com.KyKu4.MogeJlb.websocket.event.Level3ChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.Level3Event;
import com.KyKu4.MogeJlb.websocket.event.MatchExcutionChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.SnapshotEvent;
import com.KyKu4.MogeJlb.websocket.event.TickerChangeEvent;

//Public
public interface KyKu4_CJlywaTeJlb_Public extends TickerCallback {
    default void otherCallback(KucoinEvent event) {
    }

    /**
     * Symbol Snapshot
     * Topic: /market/snapshot:{symbol}
     * <p>
     * Push frequency: once every 2s
     * Subscribe to get snapshot data for a single symbol.
     */
    default void snapshotCallback(KucoinEvent<SnapshotEvent> event) {
    }

    /**
     * Level-2 Market Data
     * Topic: /market/level2:{symbol},{symbol}...
     * <p>
     * Push frequency:real-time
     * Subscribe to this topic to get Level2 order book data.
     * <p>
     * When the websocket subscription is successful, the system would send the increment change data pushed by the websocket to you.
     * <p>
     * Calibration procedure：
     * <p>
     * After receiving the websocket Level 2 data flow, cache the data.
     * Initiate a REST request to get the snapshot data of Level 2 order book.
     * Playback the cached Level 2 data flow.
     * Apply the new Level 2 data flow to the local snapshot to ensure that
     * sequenceStart(new)<=sequenceEnd+1(old) and sequenceEnd(new) > sequenceEnd(old).
     * The sequence on each record in changes only represents the last modification of the corresponding sequence of
     * the price, and does not serve as a basis for judging message continuity.
     * Update the level2 full data based on sequence according to the price and size. If the price is 0, ignore the
     * messages and update the sequence. If the size=0, update the sequence and remove the price of which the size is
     * 0 out of level 2. For other cases, please update the price.
     * <p>
     * Example:
     * * Take BTC/USDT as an example, suppose the current order book data in level 2 is as follows:
     * <p>
     * After subscribing to the channel, you would receive changes as follows:
     * <p>
     * ...
     * "asks":[
     * ["3988.59","3", 16], // ignore it because sequence = 16
     * ["3988.61","0", 19], // Remove 3988.61
     * ["3988.62","8", 15], // ignore it because sequence < 16
     * ]
     * "bids":[
     * ["3988.50", "44", "18"] // Update size of 3988.50 to 44
     * ]
     * "sequenceEnd": 15,
     * "sequenceStart": 19,
     * ...
     * <p>
     * The sequence on each record in changes only represents the last modification of the corresponding sequence
     * of the price, not as a basis for judging the continuity of the message; for example, when there are multiple
     * updates at the same price ["3988.50", "20", "17" "], ["3988.50", "44", "18"], at this time only the
     * latest ["3988.50", "44", "18"] will be pushed.
     * <p>
     * Get a snapshot of the order book through a REST request (Get Order Book) to build a local order book.
     * Suppose that data we got is as follows:
     * ...
     * "sequence": "16",
     * "asks":[
     * ["3988.62","8"],//[Price, Size]
     * ["3988.61","32"],
     * ["3988.60","47"],
     * ["3988.59","3"],
     * ]
     * "bids":[
     * ["3988.51","56"],
     * ["3988.50","15"],
     * ["3988.49","100"],
     * ["3988.48","10"]
     * ]
     * ...
     * <p>
     * The current data on the local order book is as follows:
     * <p>
     * | Price | Size | Side |
     * |---------|-----|------|
     * | 3988.62 | 8   | Sell |
     * | 3988.61 | 32  | Sell |
     * | 3988.60 | 47  | Sell |
     * | 3988.59 | 3   | Sell |
     * | 3988.51 | 56  | Buy  |
     * | 3988.50 | 15  | Buy  |
     * | 3988.49 | 100 | Buy  |
     * | 3988.48 | 10  | Buy  |
     * <p>
     * In the beginning, the sequence of the order book is 16. Discard the feed data of sequence that is below or
     * equals to 16, and apply playback the sequence [18,19] to update the snapshot of the order book.
     * Now the sequence of your order book is 19 and your local order book is up-to-date.
     * <p>
     * Diff:
     * Update size of 3988.50 to 44 (Sequence 18)
     * Remove 3988.61 (Sequence 19)
     * Now your current order book is up-to-date and final data is as follows:
     * <p>
     * | Price | Size | Side |
     * |---------|-----|------|
     * | 3988.62 | 8   | Sell |
     * | 3988.60 | 47  | Sell |
     * | 3988.59 | 3   | Sell |
     * | 3988.51 | 56  | Buy  |
     * | 3988.50 | 44  | Buy  |
     * | 3988.49 | 100 | Buy  |
     * | 3988.48 | 10  | Buy  |
     */
    default void level2Callback(KucoinEvent<Level2ChangeEvent> event) {
    }

    /**
     * Level2 - 5 best ask/bid orders
     * Topic: /spotMarket/level2Depth5:{symbol},{symbol}...
     * <p>
     * Push frequency: once every 100ms
     * The system will return the 5 best ask/bid orders data, which is the snapshot data of every 100 milliseconds (in other words, the 5 best ask/bid orders data returned every 100 milliseconds in real-time).
     */
    default void level2Depth5Callback(KucoinEvent<Level2Event> event) {
    }

    /**
     * Level2 - 50 best ask/bid orders
     * Topic: /spotMarket/level2Depth50:{symbol},{symbol}...
     * <p>
     * Push frequency: once every 100ms
     * The system will return the 50 best ask/bid orders data, which is the snapshot data of every 100 milliseconds (in other words, the 50 best ask/bid orders data returned every 100 milliseconds in real-time).
     */
    default void level2Depth50Callback(KucoinEvent<Level2Event> event) {
    }

    /**
     * Match Execution Data
     * Topic: /market/match:{symbol},{symbol}...
     * <p>
     * Push frequency: real-time
     * Subscribe to this topic to get the matching event data flow of Level 3.
     * <p>
     * For each order traded, the system would send you the match messages in the following format.
     */
    default void matchDataCallback(KucoinEvent<MatchExcutionChangeEvent> event) {
    }

    @Deprecated//??
    default void level3Callback(KucoinEvent<Level3ChangeEvent> event) {
    }

    @Deprecated//??
    default void level3V2Callback(KucoinEvent<Level3Event> event) {
    }
}

//TODO: Not implemented:
//Klines
//Index Price
//Mark Price
//Order Book Change
//Market Snapshot
