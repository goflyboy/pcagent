package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话VO - 用于前端显示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {
    private String sessionId;
    private String currentStep;
    private ProgressVO progress;
    /**
     * 显示数据，根据 currentStep 不同，包含不同类型的数据：
     * - step1: ConfigReqVO
     * - step2: SpecParseResultVO 或 ProductSelectionVO
     * - step3: ParameterConfigVO
     */
    private Object displayData;
}

