package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检查结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckResultVO {
    private Integer errorCode; // 错误代码，0表示无错误
    private String errorMessage; // 错误消息
}

