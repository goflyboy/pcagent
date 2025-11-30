package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目录节点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogNode {
    private String fatherCode;
    private String code;
    private String name;
}

