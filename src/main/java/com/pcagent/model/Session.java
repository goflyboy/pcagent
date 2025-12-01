package com.pcagent.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private String sessionId;
    private String currentStep;
    private NextAction nextAction;
    private Progress progress;

    /**
     * 用于在不同步骤中携带数据（例如 ConfigReq、规格列表、ProductConfig 等）。
     * 通过 JsonTypeInfo 保留具体类型信息，便于通过 REST 接口往返时反序列化为原始类型。
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    private Object data;
}

