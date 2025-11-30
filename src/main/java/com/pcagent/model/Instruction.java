package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指令
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instruction {
    private String title;
    private String message;
    private String action;
    private String target;
    private Object parameters;
    private String submitUrl;
}

