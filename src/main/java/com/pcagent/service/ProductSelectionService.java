package com.pcagent.service;

import com.pcagent.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pcagent.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 产品选型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSelectionService {
    private final ProductOntoService productOntoService;

    /**
     * 做产品选型
     * 
     * @param productSpecReqGroup 产品规格需求组
     * @return 产品偏离度列表和最佳产品
     */
    public Pair<List<ProductDeviationDegree>, ProductDeviationDegree> selectProduct(
            List<ProductSpecificationReq> productSpecReqGroup) {
        List<ProductDeviationDegree> productDeviationDegrees = calcProductDeviationDegrees(productSpecReqGroup);
        
        // 按deviationDegree降序排列（满足度高的在前）
        productDeviationDegrees.sort(Comparator.comparing(ProductDeviationDegree::getTotalDeviationDegrees).reversed());

        if (productDeviationDegrees.isEmpty()) {
            return new Pair<>(new ArrayList<>(), null);
        }

        return new Pair<>(productDeviationDegrees, productDeviationDegrees.get(0));
    }

    /**
     * 计算产品偏离度列表
     */
    List<ProductDeviationDegree> calcProductDeviationDegrees(List<ProductSpecificationReq> productSpecReqGroup) {
        List<ProductDeviationDegree> result = new ArrayList<>();
        
        for (ProductSpecificationReq productSpecReqGroupItem : productSpecReqGroup) {
            List<String> nodeCodes = new ArrayList<>();
            nodeCodes.add(productSpecReqGroupItem.getCatalogNode());
            List<Product> products = productOntoService.queryProductByNode(nodeCodes);
            
            for (Product product : products) {
                ProductSpecification productSpec = productOntoService.queryProductSpecification(product.getCode());
                if (productSpec != null) {
                    ProductDeviationDegree productDeviationDegree = calcProductDeviationDegree(
                            productSpec, productSpecReqGroupItem);
                    productDeviationDegree.setProductCode(product.getCode());
                    result.add(productDeviationDegree);
                }
            }
        }
        
        return result;
    }

    /**
     * 计算产品的偏离度
     */
    public ProductDeviationDegree calcProductDeviationDegree(ProductSpecification productSpec,
            ProductSpecificationReq productSpecReq) {
        ProductDeviationDegree result = new ProductDeviationDegree();
        result.setProductCode(productSpec.getProductCode());
        for (SpecificationReq specItemReq : productSpecReq.getSpecReqs()) {
            if (specItemReq.getStdSpecs().contains(Specification.NOT_FOUND) || 
                specItemReq.getStdSpecs().stream().anyMatch(s -> s == Specification.NOT_FOUND)) {
                result.addSpecItemDeviationDegree(
                        SpecItemDeviationDegree.buildNotFound(specItemReq.getOriginalSpec(), ""));
            } else {
                result.addSpecItemDeviationDegree(
                        calcSpecItemDeviationDegree(productSpec, specItemReq));
            }
        }

        // 计算result.totalDeviationDegrees（总体满足度）
        int totalSpecs = result.getSpecItemDeviationDegrees().size();
        if (totalSpecs == 0) {
            result.setTotalDeviationDegrees(0);
        } else {
            long satisfiedCount = result.getSpecItemDeviationDegrees().stream()
                    .filter(SpecItemDeviationDegree::getSatisfy)
                    .count();
            result.setTotalDeviationDegrees((int) (satisfiedCount * 100 / totalSpecs));
        }

        return result;
    }

    /**
     * 计算规格项偏离度
     */
    SpecItemDeviationDegree calcSpecItemDeviationDegree(ProductSpecification productSpec,
            SpecificationReq specItemReq) {
        String originalSpec = specItemReq.getOriginalSpec();
        for (Specification stdSpecItemReq : specItemReq.getStdSpecs()) {
            // 查找产品规格中匹配的规格项
            Specification productSpecItem = productSpec.getSpecs().stream()
                    .filter(s -> s.getSpecName().equals(stdSpecItemReq.getSpecName()))
                    .findFirst()
                    .orElse(null);

            if (productSpecItem == null) {
                log.warn("Product spec item not found: {}", stdSpecItemReq.getSpecName());
                continue;
            }

            return calcSpecItemDeviationDegree(productSpecItem, stdSpecItemReq, originalSpec);
        }

        // 如果都没找到，返回NOT_FOUND
        return SpecItemDeviationDegree.buildNotFound(originalSpec, "");
    }

    /**
     * 计算规格项偏离度（具体规格项对比）
     */
   public SpecItemDeviationDegree calcSpecItemDeviationDegree(Specification specItem, Specification stdSpecItemReq, String originalSpec) {
        SpecItemDeviationDegree result = new SpecItemDeviationDegree();
        result.setOriginalSpecReq(originalSpec);
        result.setSpecName(stdSpecItemReq.getSpecName());
        result.setStdSpecReq(stdSpecItemReq);

        try {
            // 解析数值
            double reqValue = parseValue(stdSpecItemReq.getSpecValue());
            double productValue = parseValue(specItem.getSpecValue());

            String compare = stdSpecItemReq.getCompare();
            boolean satisfy = false;
            DeviationDegree deviationDegree;

            switch (compare) {
                case ">=":
                    satisfy = productValue >= reqValue;
                    deviationDegree = satisfy ? DeviationDegree.POSITIVE : DeviationDegree.NEGATIVE;
                    break;
                case ">":
                    satisfy = productValue > reqValue;
                    deviationDegree = satisfy ? DeviationDegree.POSITIVE : DeviationDegree.NEGATIVE;
                    break;
                case "<=":
                    satisfy = productValue <= reqValue;
                    deviationDegree = satisfy ? DeviationDegree.POSITIVE : DeviationDegree.NEGATIVE;
                    break;
                case "<":
                    satisfy = productValue < reqValue;
                    deviationDegree = satisfy ? DeviationDegree.POSITIVE : DeviationDegree.NEGATIVE;
                    break;
                case "=":
                    satisfy = Math.abs(productValue - reqValue) < 0.01;
                    deviationDegree = satisfy ? DeviationDegree.NONE : DeviationDegree.NEGATIVE;
                    break;
                default:
                    satisfy = false;
                    deviationDegree = DeviationDegree.NOT_FOUND;
            }

            result.setSatisfy(satisfy);
            result.setDeviationDegree(deviationDegree);
        } catch (Exception e) {
            log.warn("Failed to compare spec values: {} vs {}", specItem.getSpecValue(), 
                    stdSpecItemReq.getSpecValue(), e);
            result.setSatisfy(false);
            result.setDeviationDegree(DeviationDegree.NOT_FOUND);
        }

        return result;
    }

    /**
     * 解析数值
     */
    private double parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        // 提取数字部分
        String numberStr = value.replaceAll("[^0-9.-]", "");
        if (numberStr.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

