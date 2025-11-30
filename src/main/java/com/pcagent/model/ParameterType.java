package com.pcagent.model;

/**
 * 参数类型枚举
 */
public enum ParameterType {
    ENUM(0, "EnumType"),
    BOOLEAN(1, "Boolean"),
    INTEGER(2, "Integer"),
    FLOAT(3, "Float"),
    STRING(5, "String");

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

