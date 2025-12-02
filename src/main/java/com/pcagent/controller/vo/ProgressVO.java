package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进度VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressVO {
    private Integer current;
    private Integer total;
    private String message;
}

