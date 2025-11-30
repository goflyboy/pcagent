package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String fatherCode;
    private String code;
    private Long id;
    private String name;
}

