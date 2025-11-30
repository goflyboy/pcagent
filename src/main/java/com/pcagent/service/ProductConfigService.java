package com.pcagent.service;

import com.pcagent.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 产品配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductConfigService {
    private final ProductOntoService productOntoService;

    /**
     * 执行参数配置
     * 
     * @param productDeviationDegree 产品偏离度
     * @param req                    配置需求
     * @return 产品配置
     */
    public ProductConfig doParameterConfigs(ProductDeviationDegree productDeviationDegree, ConfigReq req) {
        ProductParameter productPara = productOntoService.queryProductParameter(
                productDeviationDegree.getProductCode());
        
        if (productPara == null || productPara.getParas() == null || productPara.getParas().isEmpty()) {
            ProductConfig config = new ProductConfig();
            config.setProductCode(productDeviationDegree.getProductCode());
            config.setParas(new ArrayList<>());
            CheckResult checkResult = new CheckResult();
            checkResult.setLevel(CheckResultLevel.WARNING);
            checkResult.setErrorCode(1);
            checkResult.setErrorMessage("产品参数为空");
            config.setCheckResult(checkResult);
            return config;
        }

        // 参数已按sortNo排序
        List<Parameter> paras = productPara.getParas();

        // 生成配置意图
        List<ParameterConfigIntent> paraIntents = generateParameterConfigIntents(
                paras, productDeviationDegree, req.getTotalQuantity());

        // 自动配置
        Stack<ParameterConfigIntent> paraConfigResult = autoParaConfig(paraIntents);

        // 根据paraConfigResult生成ProductConfig
        ProductConfig config = new ProductConfig();
        config.setProductCode(productDeviationDegree.getProductCode());
        config.setParas(new ArrayList<>());

        for (ParameterConfigIntent intent : paraConfigResult) {
            if (intent.getResult() != null) {
                config.getParas().add(intent.getResult());
            }
        }

        // 检查结果
        CheckResult checkResult = checkParameterConfig(paraConfigResult);
        config.setCheckResult(checkResult);

        return config;
    }

    /**
     * 根据配置意图自动生成
     */
    Stack<ParameterConfigIntent> autoParaConfig(List<ParameterConfigIntent> paraIntents) {
        Stack<ParameterConfigIntent> paraConfigResultStack = new Stack<>();

        // 首次解，按第一个进行遍历
        for (ParameterConfigIntent paraIntent : paraIntents) {
            paraConfigResultStack.push(paraIntent);
            boolean hasSolution = false;

            for (ParameterConfigIntentOption intentOption : paraIntent.getIntentOptions()) {
                if (!intentOption.getIsVisited()) {
                    if (paraIntent.getResult() == null) {
                        paraIntent.setResult(new ParameterConfig());
                    }
                    paraIntent.getResult().setCode(paraIntent.getCode());
                    paraIntent.getResult().setValue(intentOption.getValue());
                    paraIntent.getResult().setInference(intentOption.getMessage());
                    intentOption.setIsVisited(true);
                    hasSolution = true;
                    break;
                }
            }

            if (!hasSolution) {
                log.warn("No solution found for parameter: {}", paraIntent.getCode());
            }
        }

        CheckResult checkResult = checkParameterConfig(paraConfigResultStack);
        if (checkResult.getLevel() == CheckResultLevel.SUCCESS) {
            return paraConfigResultStack;
        }

        // TODO: 如果检查失败，进行回溯
        // paraConfigResultStack，对每个进行回溯
        log.warn("Parameter config check failed, need backtracking");

        return paraConfigResultStack;
    }

    /**
     * 检查参数配置结果
     */
    CheckResult checkParameterConfig(Stack<ParameterConfigIntent> paraConfigResultStack) {
        // TODO: 调用LLM检查结果是否正确？
        // 目前简单检查：所有参数都有值
        for (ParameterConfigIntent intent : paraConfigResultStack) {
            if (intent.getResult() == null || intent.getResult().getValue() == null) {
                CheckResult result = new CheckResult();
                result.setLevel(CheckResultLevel.ERROR);
                result.setErrorCode(1);
                result.setErrorMessage("参数 " + intent.getCode() + " 未配置");
                return result;
            }
        }

        CheckResult result = new CheckResult();
        result.setLevel(CheckResultLevel.SUCCESS);
        result.setErrorCode(0);
        result.setErrorMessage("配置检查通过");
        return result;
    }

    /**
     * 生成配置意图
     */
    List<ParameterConfigIntent> generateParameterConfigIntents(List<Parameter> parameters,
            ProductDeviationDegree productDeviationDegree, Integer totalQuantity) {
        List<ParameterConfigIntent> paraIntents = new ArrayList<>();

        for (Parameter parameter : parameters) {
            SpecItemDeviationDegree specItemDeviation = productDeviationDegree
                    .querySpecItemDeviationDegree(parameter.getRefSpecCode());

            if (specItemDeviation == null) {
                if (parameter.getCode().contains("QTY")) {
                    paraIntents.add(generateParameterConfigIntent4Qty(parameter, totalQuantity));
                } else {
                    log.warn("No spec deviation found for parameter: {}, refSpecCode: {}", 
                            parameter.getCode(), parameter.getRefSpecCode());
                }
            } else {
                paraIntents.add(generateParameterConfigIntent(parameter, specItemDeviation));
            }
        }

        return paraIntents;
    }

    /**
     * 生成数量参数配置意图
     */
    ParameterConfigIntent generateParameterConfigIntent4Qty(Parameter parameter, Integer reqQty) {
        ParameterConfigIntent paraConfigIntent = new ParameterConfigIntent();
        paraConfigIntent.setCode(parameter.getCode());
        paraConfigIntent.setBase(parameter);
        paraConfigIntent.setIntentOptions(new ArrayList<>());

        ParameterConfigIntentOption option = new ParameterConfigIntentOption();
        option.setValue(String.valueOf(reqQty != null ? reqQty : 1));
        option.setMessage("根据需求配置数量: " + reqQty);
        option.setIsVisited(false);
        paraConfigIntent.getIntentOptions().add(option);

        return paraConfigIntent;
    }

    /**
     * 生成配置意图
     */
    ParameterConfigIntent generateParameterConfigIntent(Parameter parameter,
            SpecItemDeviationDegree specItemDeviationDegree) {
        ParameterConfigIntent paraConfigIntent = new ParameterConfigIntent();
        paraConfigIntent.setCode(parameter.getCode());
        paraConfigIntent.setBase(parameter);
        paraConfigIntent.setIntentOptions(new ArrayList<>());

        // 根据parameter.options 和 specItemDeviationDegree.stdSpecReq 来生成paraConfigIntent.options
        if (parameter.getOptions() != null && !parameter.getOptions().isEmpty()) {
            for (String optionValue : parameter.getOptions()) {
                ParameterConfigIntentOption option = new ParameterConfigIntentOption();
                option.setValue(optionValue);
                option.setMessage("根据规格需求 " + specItemDeviationDegree.getOriginalSpecReq() + 
                        " 选择配置: " + optionValue);
                option.setIsVisited(false);
                paraConfigIntent.getIntentOptions().add(option);
            }
        } else {
            // 输入型参数
            ParameterConfigIntentOption option = new ParameterConfigIntentOption();
            option.setValue(parameter.getDefaultValue());
            option.setMessage("使用默认值: " + parameter.getDefaultValue());
            option.setIsVisited(false);
            paraConfigIntent.getIntentOptions().add(option);
        }

        return paraConfigIntent;
    }
}

