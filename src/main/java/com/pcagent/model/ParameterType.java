package com.pcagent.model;

/**
 * 参数类型枚举
 */
public enum ParameterType {
    ENUMINT(5, "5-[Int]"),
    ENUMSTRING(6, "6-[String]"),
    INTEGER(0, "0-Int"),
    FLOAT(1, "1-Float"),
    STRING(3, "3-String");

    private final int code;
    private final String description;

    ParameterType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

