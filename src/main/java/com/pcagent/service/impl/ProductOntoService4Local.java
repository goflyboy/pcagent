package com.pcagent.service.impl;

import com.pcagent.model.*;
import com.pcagent.service.ProductOntoService;
import com.pcagent.util.CommHelper;
import com.pcagent.util.ProductOntoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品本体数据服务本地实现
 * 使用JSON文件存储样例数据
 */
@Slf4j
@Service
public class ProductOntoService4Local implements ProductOntoService {
    private ProductOntoData data;

    @PostConstruct
    public void init() {
        try {
            String dataPath = CommHelper.getCodeResourcePath() + "/data/product-onto-data.json";
            this.data = ProductOntoUtils.fromJsonFile(dataPath, ProductOntoData.class);
            log.info("Product ontology data loaded successfully from: {}", dataPath);
        } catch (IOException e) {
            log.error("Failed to load product ontology data", e);
            this.data = new ProductOntoData();
        }
    }

    @Override
    public List<CatalogNode> querySalesCatalogNodes(String salesCatalogId, String nodeName) {
        if (data == null || data.getCatalogNodes() == null) {
            return new ArrayList<>();
        }
        return data.getCatalogNodes().stream()
                .filter(node -> nodeName == null || node.getName().contains(nodeName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> queryProductByNode(List<String> nodeCodes) {
        if (data == null || data.getProducts() == null || nodeCodes == null || nodeCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return data.getProducts().stream()
                .filter(product -> nodeCodes.contains(product.getFatherCode()))
                .collect(Collectors.toList());
    }

    @Override
    public ProductSpecificationReq parseProductSpecs(String nodeCode, List<String> originalSpecs) {
        ProductSpecificationReq req = new ProductSpecificationReq();
        req.setCatalogNode(nodeCode);

        // 获取节点规格模板
        List<Specification> nodeSpecs = data.getNodeSpecifications().get(nodeCode);
        if (nodeSpecs == null) {
            log.warn("No specifications found for node: {}", nodeCode);
            return req;
        }

        // 解析每个原始规格
        for (String originalSpec : originalSpecs) {
            SpecificationReq specReq = new SpecificationReq();
            specReq.setOriginalSpec(originalSpec);

            // 尝试匹配节点规格模板
            for (Specification nodeSpec : nodeSpecs) {
                if (matchesSpec(originalSpec, nodeSpec)) {
                    Specification stdSpec = new Specification();
                    stdSpec.setSpecName(nodeSpec.getSpecName());
                    stdSpec.setCompare(nodeSpec.getCompare());
                    stdSpec.setUnit(nodeSpec.getUnit());
                    // 从原始规格中提取值
                    String specValue = extractSpecValue(originalSpec, nodeSpec);
                    stdSpec.setSpecValue(specValue);
                    specReq.getStdSpecs().add(stdSpec);
                }
            }

            // 如果没找到匹配的规格，添加NOT_FOUND
            if (specReq.getStdSpecs().isEmpty()) {
                specReq.getStdSpecs().add(Specification.NOT_FOUND);
            }

            req.getSpecReqs().add(specReq);
        }

        return req;
    }

    /**
     * 检查原始规格是否匹配节点规格模板
     */
    private boolean matchesSpec(String originalSpec, Specification nodeSpec) {
        String specName = nodeSpec.getSpecName();
        return originalSpec.contains(specName);
    }

    /**
     * 从原始规格中提取值
     */
    private String extractSpecValue(String originalSpec, Specification nodeSpec) {
        // 简单的提取逻辑，实际应该使用更复杂的解析
        String specName = nodeSpec.getSpecName();
        int nameIndex = originalSpec.indexOf(specName);
        if (nameIndex == -1) {
            return "";
        }

        // 尝试提取数值
        String afterName = originalSpec.substring(nameIndex + specName.length());
        // 提取操作符和数值
        if (afterName.contains("≥") || afterName.contains(">=")) {
            // 提取数值
            String[] parts = afterName.split("[≥>=]");
            if (parts.length > 1) {
                String valuePart = parts[1].trim();
                // 提取数字
                String number = valuePart.replaceAll("[^0-9.]", "");
                return number;
            }
        } else if (afterName.contains("=")) {
            String[] parts = afterName.split("=");
            if (parts.length > 1) {
                String valuePart = parts[1].trim();
                String number = valuePart.replaceAll("[^0-9.-]", "");
                return number;
            }
        }

        return "";
    }

    @Override
    public ProductSpecification queryProductSpecification(String productCode) {
        if (data == null || data.getProductSpecifications() == null) {
            return null;
        }
        return data.getProductSpecifications().get(productCode);
    }

    @Override
    public ProductParameter queryProductParameter(String productCode) {
        if (data == null || data.getProductParameters() == null) {
            return null;
        }
        ProductParameter param = data.getProductParameters().get(productCode);
        if (param != null && param.getParas() != null) {
            // 按sortNo排序
            param.getParas().sort((a, b) -> {
                if (a.getSortNo() == null) return 1;
                if (b.getSortNo() == null) return -1;
                return a.getSortNo().compareTo(b.getSortNo());
            });
        }
        return param;
    }
}

