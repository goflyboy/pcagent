package com.pcagent.exception;

/**
 * 解析产品规格异常
 */
public class ParseProductSpecException extends RuntimeException {
    public ParseProductSpecException(String message) {
        super(message);
    }

    public ParseProductSpecException(String message, Throwable cause) {
        super(message, cause);
    }
}

