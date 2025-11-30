package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检查结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {
    private CheckResultLevel level; // error
    private Integer errorCode; // 没有错误是0
    private String errorMessage;
}

