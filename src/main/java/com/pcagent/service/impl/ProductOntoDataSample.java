package com.pcagent.service.impl;

import java.util.Arrays;
import java.util.List;

/**
 * 产品本体数据样例常量
 * 用于测试和示例代码
 */
public class ProductOntoDataSample {
    
    // 目录节点
    public static final String CATALOG_NODE_DATA_CENTER_SERVER = "data_center_server";
    
    // 产品代码
    public static final String PRODUCT_CODE_POWEREDGE_R760XA = "poweredge_r760xa";
    public static final String PRODUCT_CODE_POWEREDGE_R860XA = "poweredge_r860xa";
    
    // 规格需求 - 内存
    public static final String SPEC_REQ_MEMORY_256GB = "内存:配置≥256GB DDR4 ECC Registered内存";
    public static final String SPEC_REQ_MEMORY_512GB = "内存:配置≥512GB DDR4 ECC Registered内存";
    
    // 规格需求 - CPU
    public static final String SPEC_REQ_CPU_16_CORES = "CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核 ";
    public static final String SPEC_REQ_CPU_32_CORES = "CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥32核 ";
    
    // 规格需求列表 - 常用组合
    public static final List<String> SPEC_REQ_ITEMS_MEMORY_256GB_CPU_16 = Arrays.asList(
            SPEC_REQ_MEMORY_256GB,
            SPEC_REQ_CPU_16_CORES
    );
    
    public static final List<String> SPEC_REQ_ITEMS_MEMORY_512GB_CPU_32 = Arrays.asList(
            SPEC_REQ_MEMORY_512GB,
            SPEC_REQ_CPU_32_CORES
    );
    
    // 私有构造器，防止实例化
    private ProductOntoDataSample() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

