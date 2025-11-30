package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下一步动作
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextAction {
    private String type; // 'wait' | 'collect_info' | 'execute' | 'confirm' | 'terminate'
    private Instruction instruction;
}

