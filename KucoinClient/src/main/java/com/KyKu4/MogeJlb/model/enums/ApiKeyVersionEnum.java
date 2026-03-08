package com.KyKu4.MogeJlb.model.enums;

/**
 * Created by tao.mao on 2021/3/4.
 */
public enum ApiKeyVersionEnum {

    V1(1), V2(2);

    private Integer version;

    ApiKeyVersionEnum(Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return this.version;
    }
}
