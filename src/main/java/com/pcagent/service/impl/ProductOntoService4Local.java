package com.pcagent.service.impl;

import com.pcagent.model.*;
import com.pcagent.service.ProductOntoService;
import com.pcagent.util.CommHelper;
import com.pcagent.util.ProductOntoUtils;
import com.pcagent.util.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            String dataPath = CommHelper.getCodeResourcePath() + "/data/product-onto-data-sample.json";
            this.data = ProductOntoUtils.fromJsonFile(dataPath, ProductOntoData.class);
            log.info("Product ontology data loaded successfully from: {}", dataPath);
            
            // 对 orgSpecReqToStdSpecReqMap 的 key 进行规范化处理
            normalizeOrgSpecReqToStdSpecReqMap();
        } catch (IOException e) {
            log.error("Failed to load product ontology data", e);
            this.data = new ProductOntoData();
        }
    }
    
    /**
     * 规范化 orgSpecReqToStdSpecReqMap 的 key
     * 对原始规格请求的 key 进行特殊符号处理（去掉空格，替换中文标点为英文标点）
     */
    private void normalizeOrgSpecReqToStdSpecReqMap() {
        if (data == null || data.getOrgSpecReqToStdSpecReqMap() == null) {
            return;
        }
        
        Map<String, List<Specification>> originalMap = data.getOrgSpecReqToStdSpecReqMap();
        if (originalMap.isEmpty()) {
            return;
        }
        
        Map<String, List<Specification>> normalizedMap = new HashMap<>();
        int normalizedCount = 0;
        int duplicateCount = 0;
        
        for (Map.Entry<String, List<Specification>> entry : originalMap.entrySet()) {
            String originalKey = entry.getKey();
            List<Specification> value = entry.getValue();
            
            // 规范化 key
            String normalizedKey = StringHelper.normalizeSpecReqItem(originalKey);
            
            // 如果 key 发生变化，记录日志
            if (!originalKey.equals(normalizedKey)) {
                normalizedCount++;
                log.debug("Normalized spec req key: '{}' -> '{}'", originalKey, normalizedKey);
            }
            
            // 处理 key 规范化后可能重复的情况
            if (normalizedMap.containsKey(normalizedKey)) {
                // 如果 key 重复，合并 value 列表（去重）
                List<Specification> existingSpecs = normalizedMap.get(normalizedKey);
                List<Specification> newSpecs = new ArrayList<>(existingSpecs);
                for (Specification spec : value) {
                    if (!newSpecs.contains(spec)) {
                        newSpecs.add(spec);
                    }
                }
                normalizedMap.put(normalizedKey, newSpecs);
                duplicateCount++;
                log.warn("Duplicate normalized key found: '{}' (original: '{}'), merged specifications", 
                        normalizedKey, originalKey);
            } else {
                normalizedMap.put(normalizedKey, new ArrayList<>(value));
            }
        }
        
        // 替换原来的 Map
        data.setOrgSpecReqToStdSpecReqMap(normalizedMap);
        
        log.info("Normalized orgSpecReqToStdSpecReqMap: {} keys normalized, {} duplicates merged, total keys: {}", 
                normalizedCount, duplicateCount, normalizedMap.size());
    }

    @Override
    public List<CatalogNode> querySalesCatalogNodes(String salesCatalogId, String nodeName) {
        if (data == null || data.getCatalogNodes() == null) {
            return new ArrayList<>();
        }
        if (nodeName == null) {
            return new ArrayList<>(data.getCatalogNodes());
        }
        return data.getCatalogNodes().stream()
                .filter(node -> node.getName().contains(nodeName) || 
                               (node.getCode() != null && node.getCode().contains(nodeName)))
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

    public List<Specification> parseSpecItemReq(String originalSpec) {
        if (data == null || data.getOrgSpecReqToStdSpecReqMap() == null) {
            return new ArrayList<>();
        }
        // 对输入的 originalSpec 也进行规范化，以便匹配规范化后的 key
        String normalizedSpec = StringHelper.normalizeSpecReqItem(originalSpec);
        List<Specification> result = data.getOrgSpecReqToStdSpecReqMap().get(normalizedSpec);
        return result != null ? result : new ArrayList<>();
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
            List<Specification> specItemReqs = parseSpecItemReq(originalSpec);
            specReq.setStdSpecs(specItemReqs); 
            req.getSpecReqs().add(specReq);
        }

        return req;
    }

    /**
     * 检查原始规格是否匹配节点规格模板
     */
    private boolean matchesSpec(String originalSpec, Specification nodeSpec) {
        //这里按originalSpec和nodeSpec的":“分割，比较前半部分是否相等
        String[] originalSpecParts = originalSpec.split(":");
        String[] nodeSpecParts = nodeSpec.getSpecName().split(":");
        if (originalSpecParts.length != 2 || nodeSpecParts.length != 2) {
            return false;
        }
        return originalSpecParts[0].equals(nodeSpecParts[0]);      
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

    /**
     * Mock规格匹配 - 通过JSON字符串数组
     * @param orgSpecDesc 原始规格描述
     * @param stdSpecJsonStrings JSON字符串数组，每个字符串代表一个Specification对象
     */
    public void mockSpecMatch(String orgSpecDesc, String... stdSpecJsonStrings) {
        try {
            List<Specification> stdSpecs = new ArrayList<>();
            for (String jsonString : stdSpecJsonStrings) {
                // 去除首尾空白字符（处理多行字符串）
                String trimmedJson = jsonString.trim();
                Specification spec = ProductOntoUtils.fromJsonString(trimmedJson, Specification.class);
                stdSpecs.add(spec);
            }
            mockSpecMatch(orgSpecDesc, stdSpecs.toArray(new Specification[0]));
        } catch (IOException e) {
            log.error("Failed to parse JSON string to Specification", e);
            throw new RuntimeException("Failed to parse JSON string to Specification", e);
        }
    }

    /**
     * Mock规格匹配 - 通过Specification对象数组
     * @param orgSpecDesc 原始规格描述
     * @param stdSpecs Specification对象数组
     */
    public void mockSpecMatch(String orgSpecDesc, Specification... stdSpecs) {
        if (data == null) {
            data = new ProductOntoData();
        }
        if (data.getOrgSpecReqToStdSpecReqMap() == null) {
            data.setOrgSpecReqToStdSpecReqMap(new java.util.HashMap<>());
        }
        List<Specification> specList = new ArrayList<>(Arrays.asList(stdSpecs));
        data.getOrgSpecReqToStdSpecReqMap().put(orgSpecDesc, specList);
        log.debug("Mocked spec match: {} -> {} specifications", orgSpecDesc, specList.size());
    }
}

