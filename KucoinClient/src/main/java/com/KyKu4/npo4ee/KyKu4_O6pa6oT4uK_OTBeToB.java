package com.KyKu4.npo4ee;

import com.KyKu4.MogeJlb.websocket.event.AccountChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.KucoinEvent;
import com.KyKu4.MogeJlb.websocket.event.Level2ChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.Level2Event;
import com.KyKu4.MogeJlb.websocket.event.Level3ChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.Level3Event;
import com.KyKu4.MogeJlb.websocket.event.MatchExcutionChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.OrderActivateEvent;
import com.KyKu4.MogeJlb.websocket.event.OrderChangeEvent;
import com.KyKu4.MogeJlb.websocket.event.SnapshotEvent;
import com.KyKu4.MogeJlb.websocket.event.StopOrderEvent;
import com.KyKu4.MogeJlb.websocket.event.TickerChangeEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
//
public class KyKu4_O6pa6oT4uK_OTBeToB extends WebSocketListener {
    final static Logger LOGGER = LoggerFactory.getLogger(KyKu4_O6pa6oT4uK_OTBeToB.class);

    private static final String TYPE = "type";
    private static final String MESSAGE = "message";
    private static final String TOPIC = "topic";

    private static final String TICKER = "/market/ticker:";
    private static final String LEVEL2 = "/market/level2:";
    private static final String MATCH = "/market/match:";
    private static final String MARKET_LEVEL3 = "/market/level3:";
    private static final String SNAPSHOT = "/market/snapshot:";
    private static final String SPOT_LEVEL3 = "/spotMarket/level3:";
    private static final String LEVEL2_DEPTH5 = "/spotMarket/level2Depth5:";
    private static final String LEVEL2_DEPTH50 = "/spotMarket/level2Depth50:";

    private static final String BALANCE = "/account/balance";
    private static final String TRADE_ORDERS = "/spotMarket/tradeOrders";
    private static final String ADVANCED_ORDERS = "/spotMarket/advancedOrders";
    private static final String SUBJECT = "subject";
    private static final String STOP_ORDER = "stopOrder";

    @Nullable
    final KyKu4_CJlywaTeJlb_Public kyKu4CJlywaTeJlbPublic;
    @Nullable
    final KyKu4_CJlywaTeJlb_Private kyKu4CJlywaTeJlbPrivate;
    final KyKu4_CJlywaTeJlb_CoeguHeHuR cJlywaTeJlb_CoeguHeHuR;

    public KyKu4_O6pa6oT4uK_OTBeToB(KyKu4_CJlywaTeJlb_Public kyKu4CJlywaTeJlbPublic, KyKu4_CJlywaTeJlb_CoeguHeHuR cJlywaTeJlb_CoeguHeHuR) {
        this.kyKu4CJlywaTeJlbPublic = kyKu4CJlywaTeJlbPublic;
        this.kyKu4CJlywaTeJlbPrivate = null;
        this.cJlywaTeJlb_CoeguHeHuR = cJlywaTeJlb_CoeguHeHuR;
    }

    public KyKu4_O6pa6oT4uK_OTBeToB(KyKu4_CJlywaTeJlb_Private kyKu4CJlywaTeJlbPrivate, KyKu4_CJlywaTeJlb_CoeguHeHuR cJlywaTeJlb_CoeguHeHuR) {
        this.kyKu4CJlywaTeJlbPrivate = kyKu4CJlywaTeJlbPrivate;
        this.kyKu4CJlywaTeJlbPublic = null;
        this.cJlywaTeJlb_CoeguHeHuR = cJlywaTeJlb_CoeguHeHuR;
    }

    private JsonNode tree(String text) {
        try {
            return KyKu4_XTTn.OBJECT_MAPPER.readTree(text);
        } catch (IOException ie) {
            throw new RuntimeException("Failed to deserialize message: " + text, ie);
        }
    }

    private <T> T deserialize(String text, TypeReference<T> typeReference) {
        try {
            return KyKu4_XTTn.OBJECT_MAPPER.readValue(text, typeReference);
        } catch (IOException ie) {
            throw new RuntimeException("Failed to deserialize message: " + text, ie);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            JsonNode jsonObject = this.tree(text);
            String type = jsonObject.get(TYPE).asText();

            if (!type.equals(MESSAGE)) {
                if (kyKu4CJlywaTeJlbPublic != null) {
                    kyKu4CJlywaTeJlbPublic.otherCallback(this.deserialize(text, new TypeReference<KucoinEvent>() {
                    }));
                } else if (kyKu4CJlywaTeJlbPrivate != null) {
                    kyKu4CJlywaTeJlbPrivate.otherCallback(this.deserialize(text, new TypeReference<KucoinEvent>() {
                    }));
                }
            } else {
                String topic = jsonObject.get(TOPIC).asText();

                if (kyKu4CJlywaTeJlbPublic != null) {
                    //Public
                    if (topic.contains(TICKER)) {
                        kyKu4CJlywaTeJlbPublic.tickerCallback(this.deserialize(text, new TypeReference<KucoinEvent<TickerChangeEvent>>() {
                        }));
                    } else if (topic.contains(LEVEL2)) {
                        kyKu4CJlywaTeJlbPublic.level2Callback(this.deserialize(text, new TypeReference<KucoinEvent<Level2ChangeEvent>>() {
                        }));
                    } else if (topic.contains(MATCH)) {
                        kyKu4CJlywaTeJlbPublic.matchDataCallback(this.deserialize(text, new TypeReference<KucoinEvent<MatchExcutionChangeEvent>>() {
                        }));
                    } else if (topic.contains(SNAPSHOT)) {
                        kyKu4CJlywaTeJlbPublic.snapshotCallback(this.deserialize(text, new TypeReference<KucoinEvent<SnapshotEvent>>() {
                        }));
                    } else if (topic.contains(LEVEL2_DEPTH5)) {
                        kyKu4CJlywaTeJlbPublic.level2Depth5Callback(this.deserialize(text, new TypeReference<KucoinEvent<Level2Event>>() {
                        }));
                    } else if (topic.contains(LEVEL2_DEPTH50)) {
                        kyKu4CJlywaTeJlbPublic.level2Depth50Callback(this.deserialize(text, new TypeReference<KucoinEvent<Level2Event>>() {
                        }));
                    } else
                        //Deprecated?
                        if (topic.contains(MARKET_LEVEL3)) {
                            kyKu4CJlywaTeJlbPublic.level3Callback(this.deserialize(text, new TypeReference<KucoinEvent<Level3ChangeEvent>>() {
                            }));
                        } else if (topic.contains(SPOT_LEVEL3)) {
                            kyKu4CJlywaTeJlbPublic.level3V2Callback(this.deserialize(text, new TypeReference<KucoinEvent<Level3Event>>() {
                            }));
                        }
                } else if (kyKu4CJlywaTeJlbPrivate != null) {
                    //Private
                    if (topic.contains(BALANCE)) {
                        kyKu4CJlywaTeJlbPrivate.accountChangeCallback(this.deserialize(text, new TypeReference<KucoinEvent<AccountChangeEvent>>() {
                        }));
                    } else if (topic.contains(TRADE_ORDERS)) {
                        kyKu4CJlywaTeJlbPrivate.orderChangeCallback(this.deserialize(text, new TypeReference<KucoinEvent<OrderChangeEvent>>() {
                        }));
                    } else if (topic.contains(ADVANCED_ORDERS)) {
                        String subject = jsonObject.get(SUBJECT).asText();
                        if (Objects.equals(subject, STOP_ORDER)) {
                            kyKu4CJlywaTeJlbPrivate.advancedOrderCallback(this.deserialize(text, new TypeReference<KucoinEvent<StopOrderEvent>>() {
                            }));
                        } else {
                            //TODO: what other advancedOrders messages can arrive ???
                            LOGGER.info("Unrecognized advanced order message arrived, ignored: {}", text);
                        }
                    } else
                        //Deprecated?
                        if (topic.contains(MARKET_LEVEL3)) {
                            kyKu4CJlywaTeJlbPrivate.orderActivateCallback(this.deserialize(text, new TypeReference<KucoinEvent<OrderActivateEvent>>() {
                            }));
                        }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing message {}", text, e);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        cJlywaTeJlb_CoeguHeHuR.onOpen(webSocket, response);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        cJlywaTeJlb_CoeguHeHuR.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        cJlywaTeJlb_CoeguHeHuR.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
        cJlywaTeJlb_CoeguHeHuR.onFailure(webSocket, t, response);
    }
}
