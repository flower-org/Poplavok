/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.KyKu4.MogeJlb.model.InstanceServer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by chenshiwei on 2019/1/15.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableWebsocketTokenResponse.class)
@JsonDeserialize(as = ImmutableWebsocketTokenResponse.class)
public interface WebsocketTokenResponse {

    List<InstanceServer> instanceServers();

    @Nullable
    String token();

}
