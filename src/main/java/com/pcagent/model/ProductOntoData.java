package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品本体数据
 * 用于存储和加载产品本体数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOntoData {
    // 目录节点列表
    private List<CatalogNode> catalogNodes = new ArrayList<>();
    
    // 产品列表
    private List<Product> products = new ArrayList<>();
    
    // 产品规格映射：productCode -> ProductSpecification
    private Map<String, ProductSpecification> productSpecifications = new HashMap<>();
    
    // 产品参数映射：productCode -> ProductParameter
    private Map<String, ProductParameter> productParameters = new HashMap<>();
    
    // 目录节点规格映射：nodeCode -> List<Specification>
    private Map<String, List<Specification>> nodeSpecifications = new HashMap<>();
}

