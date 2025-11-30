package com.pcagent.service;

import com.pcagent.exception.ParseProductSpecException;
import com.pcagent.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品规格解析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSpecificationParserService {
    private final ProductOntoService productOntoService;

    /**
     * 规格解析
     * 
     * @param catalogNode   目录节点
     * @param specReqItems  规格需求项列表
     * @return 产品规格需求
     * @throws ParseProductSpecException 解析失败时抛出
     */
    public ProductSpecificationReq parseProductSpecsByCatalogNode(String catalogNode, List<String> specReqItems) {
        try {
            return productOntoService.parseProductSpecs(catalogNode, specReqItems);
        } catch (Exception e) {
            log.error("Failed to parse product specs for node: {}", catalogNode, e);
            throw new ParseProductSpecException("Failed to parse product specs: " + e.getMessage(), e);
        }
    }

    /**
     * 规格解析 - 根据产品系列解析
     * 
     * @param productSerial 产品系列
     * @param specReqItems  规格需求项列表
     * @return 产品规格需求列表
     * @throws ParseProductSpecException 解析失败时抛出
     */
    public List<ProductSpecificationReq> parseProductSpecs(String productSerial, List<String> specReqItems) {
        try {
            List<ProductSpecificationReq> result = new ArrayList<>();

            // 查询目录节点
            List<CatalogNode> catalogNodes = productOntoService.querySalesCatalogNodes("001", productSerial);
            if (catalogNodes.isEmpty()) {
                log.warn("No catalog nodes found for product serial: {}", productSerial);
                throw new ParseProductSpecException("No catalog nodes found for product serial: " + productSerial);
            }

            // 对每个目录节点解析规格
            for (CatalogNode catalogNode : catalogNodes) {
                ProductSpecificationReq req = productOntoService.parseProductSpecs(catalogNode.getCode(), specReqItems);
                // 如果找不到，则返回Specification.NOT_FOUND
                result.add(req);
            }

            return result;
        } catch (ParseProductSpecException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse product specs for serial: {}", productSerial, e);
            throw new ParseProductSpecException("Failed to parse product specs: " + e.getMessage(), e);
        }
    }
}

