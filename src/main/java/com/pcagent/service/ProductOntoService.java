package com.pcagent.service;

import com.pcagent.model.*;

import java.util.List;

/**
 * 产品本体数据服务接口
 */
public interface ProductOntoService {
    /**
     * 按销售目录节点名称查找目录节点
     * 
     * @param salesCatalogId 销售目录ID
     * @param nodeName       节点名称（模糊搜索）
     * @return 目录节点列表
     */
    List<CatalogNode> querySalesCatalogNodes(String salesCatalogId, String nodeName);

    /**
     * 按目录节点获取产品列表
     * 
     * @param nodeCodes 节点代码列表
     * @return 产品列表
     */
    List<Product> queryProductByNode(List<String> nodeCodes);

    /**
     * 解析规格
     * 
     * @param nodeCode      节点代码
     * @param originalSpecs 原始规格列表
     * @return 产品规格需求
     */
    ProductSpecificationReq parseProductSpecs(String nodeCode, List<String> originalSpecs);

    /**
     * 获取指定产品的规格列表
     * 
     * @param productCode 产品代码
     * @return 产品规格
     */
    ProductSpecification queryProductSpecification(String productCode);

    /**
     * 获取指定产品的参数列表
     * 
     * @param productCode 产品代码
     * @return 产品参数
     */
    ProductParameter queryProductParameter(String productCode);
}

