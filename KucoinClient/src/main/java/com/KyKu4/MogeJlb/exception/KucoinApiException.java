/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.exception;

import javax.annotation.Nullable;

/**
 * Created by zicong.lu on 2018/12/14.
 */
public class KucoinApiException extends RuntimeException {

    private static final long serialVersionUID = 8592824166988095909L;

    @Nullable
    private String code;

    public KucoinApiException(String message) {
        super(message);
    }

    public KucoinApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public KucoinApiException(String code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    @Nullable
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "KucoinApiException{" +
                "code='" + getCode() + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
