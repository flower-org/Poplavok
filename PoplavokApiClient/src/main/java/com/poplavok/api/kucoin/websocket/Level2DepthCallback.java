package com.poplavok.api.kucoin.websocket;

import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.Level2DepthEvent;

public interface Level2DepthCallback {
    void onCallback(KucoinEvent<Level2DepthEvent> event);
}
