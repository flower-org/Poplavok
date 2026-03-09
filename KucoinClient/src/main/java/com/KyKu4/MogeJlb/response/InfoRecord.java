package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableInfoRecord.class)
@JsonDeserialize(as = ImmutableInfoRecord.class)
public interface InfoRecord {
    @Nullable
    String id();
    @Nullable
    String type();
    @Nullable
    String text();
    @Nullable
    String href();
    @Nullable
    String subText();
    @Nullable
    String subHref();
    @Nullable
    String src();
    @Nullable
    String remark();
    @Nullable
    Boolean machineTranslate();
}
