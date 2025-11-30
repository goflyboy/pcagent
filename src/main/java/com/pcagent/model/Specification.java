package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Specification {
    private String specName;
    private String compare; // 操作符：">", ">=", "=", "<", "<="
    private String specValue; // 值：数字类型如"1"，字符类型如"GE"，列表类型如"大,中,小"
    private String unit;
    private ParameterType type = ParameterType.INTEGER; // 参数类型，默认INTEGER

    public static final Specification NOT_FOUND = new Specification("NOT_FOUND", "", "", "", ParameterType.INTEGER);

    public Specification(String specName) {
        this.specName = specName;
        this.type = ParameterType.INTEGER;
    }
}

