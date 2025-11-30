package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进度
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Progress {
    private Integer current;
    private Integer total;
    private String message;
}

