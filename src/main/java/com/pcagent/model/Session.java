package com.pcagent.model;

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
    private Object data;
}

