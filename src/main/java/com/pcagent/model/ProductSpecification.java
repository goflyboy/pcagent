package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品规格
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecification {
    private String productCode;
    private List<Specification> specs = new ArrayList<>();
}

