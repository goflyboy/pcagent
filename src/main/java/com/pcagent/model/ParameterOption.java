package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数选项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterOption {
    private String code;
    private String value;
    private int sortNo = 0;
}

