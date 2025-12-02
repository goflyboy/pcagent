package com.pcagent.controller;

import com.pcagent.controller.vo.*;
import com.pcagent.model.*;
import com.pcagent.service.ProductOntoService;
import com.pcagent.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Session 到 SessionVO 的转换器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionVOConverter {
    private final ProductOntoService productOntoService;

    /**
     * 将 Session 转换为 SessionVO
     */
    public SessionVO convert(Session session) {
        if (session == null) {
            return null;
        }

        SessionVO vo = new SessionVO();
        vo.setSessionId(session.getSessionId());
        vo.setCurrentStep(session.getCurrentStep());

        // 转换进度
        if (session.getProgress() != null) {
            ProgressVO progressVO = new ProgressVO();
            progressVO.setCurrent(session.getProgress().getCurrent());
            progressVO.setTotal(session.getProgress().getTotal());
            progressVO.setMessage(session.getProgress().getMessage());
            vo.setProgress(progressVO);
        }

        // 根据步骤转换显示数据
        String step = session.getCurrentStep();
        Object data = session.getData();
        if (data != null) {
            if (Plan.STEP1.equals(step) && data instanceof ConfigReq) {
                vo.setDisplayData(convertConfigReqToVO((ConfigReq) data));
            } else if (Plan.STEP2.equals(step)) {
                if (data instanceof List) {
                    // 规格解析结果
                    @SuppressWarnings("unchecked")
                    List<ProductSpecificationReq> specReqs = (List<ProductSpecificationReq>) data;
                    vo.setDisplayData(convertSpecParseResultToVO(specReqs));
                } else if (data instanceof Pair) {
                    // 产品选型结果
                    @SuppressWarnings("unchecked")
                    Pair<List<ProductDeviationDegree>, ProductDeviationDegree> selectionResult = 
                            (Pair<List<ProductDeviationDegree>, ProductDeviationDegree>) data;
                    vo.setDisplayData(convertProductSelectionToVO(selectionResult));
                }
            } else if (Plan.STEP3.equals(step) && data instanceof ProductConfig) {
                vo.setDisplayData(convertParameterConfigToVO((ProductConfig) data));
            }
        }

        return vo;
    }

    /**
     * 转换 ConfigReq 为 ConfigReqVO
     */
    private ConfigReqVO convertConfigReqToVO(ConfigReq configReq) {
        ConfigReqVO vo = new ConfigReqVO();
        vo.setProductSerial(configReq.getProductSerial());
        vo.setTotalQuantity(configReq.getTotalQuantity());
        vo.setSpecReqItems(configReq.getSpecReqItems());
        vo.setTotalQuantityMemo(configReq.getTotalQuantityMemo());

        // 转换配置策略为显示名称
        if (configReq.getConfigStrategy() != null) {
            switch (configReq.getConfigStrategy()) {
                case PRICE_MIN_PRIORITY:
                    vo.setConfigStrategy("目标价最小优先");
                    break;
                case TECH_MAX_PRIORITY:
                    vo.setConfigStrategy("技术最大优先");
                    break;
                default:
                    vo.setConfigStrategy(configReq.getConfigStrategy().name());
            }
        }

        return vo;
    }

    /**
     * 转换规格解析结果为 SpecParseResultVO
     */
    private SpecParseResultVO convertSpecParseResultToVO(List<ProductSpecificationReq> specReqs) {
        SpecParseResultVO vo = new SpecParseResultVO();
        List<SpecParseItemVO> items = new ArrayList<>();

        int index = 1;
        for (ProductSpecificationReq productSpecReq : specReqs) {
            for (SpecificationReq specReq : productSpecReq.getSpecReqs()) {
                SpecParseItemVO item = new SpecParseItemVO();
                item.setIndex(index++);
                item.setOriginalSpec(specReq.getOriginalSpec());

                // 格式化标准规格需求
                if (specReq.getStdSpecs() != null && !specReq.getStdSpecs().isEmpty()) {
                    String stdSpecText = specReq.getStdSpecs().stream()
                            .map(spec -> formatSpecification(spec))
                            .collect(Collectors.joining(" 或 "));
                    item.setStdSpec(stdSpecText);
                } else {
                    item.setStdSpec("未解析");
                }

                items.add(item);
            }
        }

        vo.setItems(items);
        return vo;
    }

    /**
     * 格式化规格为显示文本
     */
    private String formatSpecification(Specification spec) {
        if (spec == null) {
            return "";
        }
        String specName = spec.getSpecName() != null ? spec.getSpecName() : "";
        String compare = spec.getCompare() != null ? spec.getCompare() : "";
        String specValue = spec.getSpecValue() != null ? spec.getSpecValue() : "";
        String unit = spec.getUnit() != null ? spec.getUnit() : "";

        if (specName.contains(":")) {
            String[] parts = specName.split(":", 2);
            if (parts.length == 2) {
                return parts[1] + compare + specValue + unit;
            }
        }
        return specName + compare + specValue + unit;
    }

    /**
     * 转换产品选型结果为 ProductSelectionVO
     */
    private ProductSelectionVO convertProductSelectionToVO(
            Pair<List<ProductDeviationDegree>, ProductDeviationDegree> selectionResult) {
        ProductSelectionVO vo = new ProductSelectionVO();

        ProductDeviationDegree selected = selectionResult.getSecond();
        if (selected != null) {
            vo.setSelectedProductCode(selected.getProductCode());
            vo.setSelectedProductName(getProductName(selected.getProductCode()));

            // 转换偏离度详情
            if (selected.getSpecItemDeviationDegrees() != null) {
                List<SpecDeviationItemVO> deviationItems = new ArrayList<>();
                int index = 1;
                for (SpecItemDeviationDegree item : selected.getSpecItemDeviationDegrees()) {
                    SpecDeviationItemVO deviationItem = new SpecDeviationItemVO();
                    deviationItem.setIndex(index++);
                    deviationItem.setOriginalSpecReq(item.getOriginalSpecReq());
                    deviationItem.setStdSpecReq(formatSpecification(item.getStdSpecReq()));
                    deviationItem.setProductSpecValue(
                            item.getStdSpecReq() != null && item.getStdSpecReq().getSpecValue() != null
                                    ? item.getStdSpecReq().getSpecValue() + 
                                      (item.getStdSpecReq().getUnit() != null ? item.getStdSpecReq().getUnit() : "")
                                    : "");
                    deviationItem.setSatisfy(item.getSatisfy());

                    // 转换偏离类型
                    if (item.getDeviationDegree() != null) {
                        switch (item.getDeviationDegree()) {
                            case POSITIVE:
                                deviationItem.setDeviationType("正偏离");
                                break;
                            case NEGATIVE:
                                deviationItem.setDeviationType("负偏离");
                                break;
                            case NONE:
                                deviationItem.setDeviationType("无偏离");
                                break;
                            default:
                                deviationItem.setDeviationType("未找到");
                        }
                    }

                    deviationItems.add(deviationItem);
                }
                vo.setDeviationDetails(deviationItems);
            }
        }

        // 转换候选产品列表（Top3）
        List<ProductDeviationDegree> candidates = selectionResult.getFirst();
        if (candidates != null) {
            List<ProductSelectionItemVO> candidateItems = new ArrayList<>();
            int rank = 1;
            for (ProductDeviationDegree candidate : candidates) {
                ProductSelectionItemVO item = new ProductSelectionItemVO();
                item.setRank(rank++);
                item.setProductCode(candidate.getProductCode());
                item.setProductName(getProductName(candidate.getProductCode()));
                item.setDeviationDegree(candidate.getTotalDeviationDegrees());

                // 生成说明（如果有负偏离）
                List<String> descriptions = new ArrayList<>();
                if (candidate.getSpecItemDeviationDegrees() != null) {
                    for (SpecItemDeviationDegree specItem : candidate.getSpecItemDeviationDegrees()) {
                        if (specItem.getDeviationDegree() == DeviationDegree.NEGATIVE) {
                            descriptions.add(specItem.getSpecName() + "负偏离");
                        }
                    }
                }
                item.setDescription(descriptions.isEmpty() ? "" : String.join("，", descriptions));

                candidateItems.add(item);
            }
            vo.setCandidates(candidateItems);
        }

        return vo;
    }

    /**
     * 转换参数配置结果为 ParameterConfigVO
     */
    private ParameterConfigVO convertParameterConfigToVO(ProductConfig productConfig) {
        ParameterConfigVO vo = new ParameterConfigVO();
        vo.setProductCode(productConfig.getProductCode());
        vo.setProductName(getProductName(productConfig.getProductCode()));

        // 转换参数配置项
        if (productConfig.getParas() != null) {
            List<ParameterConfigItemVO> items = new ArrayList<>();
            int index = 1;

            // 获取产品参数定义，用于获取参数名称和关联的规格需求
            ProductParameter productParameter = productOntoService.queryProductParameter(productConfig.getProductCode());
            Map<String, Parameter> parameterMap = new HashMap<>();
            if (productParameter != null && productParameter.getParas() != null) {
                for (Parameter param : productParameter.getParas()) {
                    parameterMap.put(param.getCode(), param);
                }
            }

            for (ParameterConfig paraConfig : productConfig.getParas()) {
                ParameterConfigItemVO item = new ParameterConfigItemVO();
                item.setIndex(index++);
                item.setParameterCode(paraConfig.getCode());
                item.setValue(paraConfig.getValue());

                // 获取参数名称
                Parameter param = parameterMap.get(paraConfig.getCode());
                if (param != null) {
                    item.setParameterName(param.getName() != null && !param.getName().isEmpty() 
                            ? param.getName() : param.getCode());
                    
                    // 获取关联的规格需求（通过 refSpecCode）
                    if (param.getRefSpecCode() != null && !param.getRefSpecCode().isEmpty()) {
                        item.setConfigReq(param.getRefSpecCode());
                    } else {
                        item.setConfigReq("");
                    }
                } else {
                    item.setParameterName(paraConfig.getCode());
                    item.setConfigReq("");
                }

                items.add(item);
            }

            vo.setItems(items);
        }

        // 转换检查结果
        if (productConfig.getCheckResult() != null) {
            CheckResultVO checkResultVO = new CheckResultVO();
            checkResultVO.setErrorCode(productConfig.getCheckResult().getErrorCode());
            checkResultVO.setErrorMessage(productConfig.getCheckResult().getErrorMessage());
            vo.setCheckResult(checkResultVO);
        }

        return vo;
    }

    /**
     * 根据产品代码获取产品名称
     */
    private String getProductName(String productCode) {
        if (productCode == null) {
            return "";
        }
        try {
            // 从 ProductOntoService 获取产品信息
            // 由于没有直接根据 code 查询的方法，我们需要从所有产品中查找
            // 这里简化处理，实际可以通过扩展 ProductOntoService 接口来优化
            List<Product> allProducts = productOntoService.queryProductByNode(
                    java.util.Arrays.asList("data_center_server", "home_server", "onu"));
            for (Product product : allProducts) {
                if (productCode.equals(product.getCode())) {
                    return product.getName() != null ? product.getName() : productCode;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get product name for code: {}", productCode, e);
        }
        return productCode;
    }
}

