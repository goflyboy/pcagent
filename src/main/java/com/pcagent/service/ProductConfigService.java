package com.pcagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagent.model.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * 产品配置服务
 */
@Slf4j
@Service
public class ProductConfigService {
    private final ProductOntoService productOntoService;
    private final LLMInvoker llmInvoker;
    private final ObjectMapper objectMapper;
    private final Configuration freeMarkerConfig;

    @Autowired(required = false)
    public ProductConfigService(ProductOntoService productOntoService, LLMInvoker llmInvoker) {
        this.productOntoService = productOntoService;
        this.llmInvoker = llmInvoker;
        this.objectMapper = new ObjectMapper();
        // 初始化 FreeMarker 配置
        this.freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
        this.freeMarkerConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.freeMarkerConfig.setClassLoaderForTemplateLoading(
                Thread.currentThread().getContextClassLoader(), "templates");
        this.freeMarkerConfig.setAPIBuiltinEnabled(true);
    }

    public ProductConfigService(ProductOntoService productOntoService) {
        this(productOntoService, null);
    }

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
        paras.sort(Comparator.comparingInt(Parameter::getSortNo));

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
   public CheckResult checkParameterConfig(Stack<ParameterConfigIntent> paraConfigResultStack) {
        // 首先进行简单检查：所有参数都有值
        for (ParameterConfigIntent intent : paraConfigResultStack) {
            if (intent.getResult() == null || intent.getResult().getValue() == null) {
                CheckResult result = new CheckResult();
                result.setLevel(CheckResultLevel.ERROR);
                result.setErrorCode(1);
                result.setErrorMessage("参数 " + intent.getCode() + " 未配置");
                return result;
            }
        }

        // 如果有 LLM 可用，使用 LLM 进行深度检查
        if (llmInvoker != null) {
            try {
                // 构造配置知识（从 Parameter 的 checkRule）
                StringBuilder configKnowledge = new StringBuilder();
                for (ParameterConfigIntent intent : paraConfigResultStack) {
                    Parameter base = intent.getBase();
                    if (base != null && base.getCheckRule() != null && !base.getCheckRule().trim().isEmpty()) {
                        configKnowledge.append(base.getCode()).append("(").append(base.getCode()).append("):\n");
                        configKnowledge.append(base.getCheckRule()).append("\n\n");
                    }
                }

                // 如果没有任何 checkRule，则使用简单检查
                if (configKnowledge.length() == 0) {
                    log.debug("No checkRule found, using simple check");
                    CheckResult result = new CheckResult();
                    result.setLevel(CheckResultLevel.SUCCESS);
                    result.setErrorCode(0);
                    result.setErrorMessage("配置检查通过");
                    return result;
                }

                // 构造配置结果
                StringBuilder configResult = new StringBuilder();
                for (ParameterConfigIntent intent : paraConfigResultStack) {
                    if (intent.getResult() != null) {
                        configResult.append(intent.getResult().getCode())
                                .append(" = ")
                                .append("\"").append(intent.getResult().getValue()).append("\"")
                                .append("\n");
                    }
                }

                // 调用 LLM 检查
                CheckResult llmResult = checkConfigWithLLM(configKnowledge.toString(), configResult.toString());
                if (llmResult != null) {
                    return llmResult;
                }
            } catch (Exception e) {
                log.warn("Failed to check config with LLM, fallback to simple check", e);
            }
        }

        // 默认返回成功
        CheckResult result = new CheckResult();
        result.setLevel(CheckResultLevel.SUCCESS);
        result.setErrorCode(0);
        result.setErrorMessage("配置检查通过");
        return result;
    }

    /**
     * 使用 LLM 检查配置
     */
    private CheckResult checkConfigWithLLM(String configKnowledge, String configResult) {
        try {
            // 渲染 prompt
            String prompt = renderConfigCheckPrompt(configKnowledge, configResult);
            log.debug("Rendered config check prompt: {}", prompt);

            // 调用 LLM
            String response = llmInvoker.callLLMForCheck(prompt);
            log.debug("LLM check response: {}", response);

            // 解析 JSON 响应
            CheckResult checkResult = parseCheckResult(response);
            if (checkResult == null) {
                log.warn("LLM response is not a valid CheckResult JSON, fallback to simple check. response={}", response);
                return null;
            }

            // 根据 errorCode 设置 level
            if (checkResult.getErrorCode() == null || checkResult.getErrorCode() == 0) {
                checkResult.setLevel(CheckResultLevel.SUCCESS);
            } else {
                checkResult.setLevel(CheckResultLevel.ERROR);
            }

            return checkResult;

        } catch (Exception e) {
            log.error("Failed to check config with LLM", e);
            return null;
        }
    }

    /**
     * 渲染配置检查 prompt 模板
     */
    private String renderConfigCheckPrompt(String configKnowledge, String configResult) {
        try {
            Template template = freeMarkerConfig.getTemplate("config-check.ftl", StandardCharsets.UTF_8.name());
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("configKnowledge", configKnowledge != null ? configKnowledge : "");
            dataModel.put("configResult", configResult != null ? configResult : "");

            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return writer.toString();
        } catch (TemplateException | IOException e) {
            log.error("Failed to render config check prompt template", e);
            throw new IllegalStateException("Unable to render config check prompt template", e);
        }
    }

    /**
     * 解析 LLM 返回的 CheckResult JSON
     */
    private CheckResult parseCheckResult(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        try {
            // 尝试提取 JSON（可能包含 markdown 代码块）
            String json = extractJson(response);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            
            CheckResult result = new CheckResult();
            Object errorCodeObj = map.get("errorCode");
            if (errorCodeObj instanceof Number) {
                result.setErrorCode(((Number) errorCodeObj).intValue());
            } else if (errorCodeObj instanceof String) {
                result.setErrorCode(Integer.parseInt((String) errorCodeObj));
            }
            
            Object errorMessageObj = map.get("errorMessage");
            if (errorMessageObj != null) {
                result.setErrorMessage(errorMessageObj.toString());
            } else {
                result.setErrorMessage("");
            }
            
            return result;
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse LLM response as CheckResult JSON", e);
            return null;
        }
    }

    /**
     * 从响应中提取 JSON（可能包含在 markdown 代码块中）
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        // 如果包含 markdown 代码块，提取其中的内容
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("{");
            int end = trimmed.lastIndexOf("}");
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        // 直接查找 JSON 对象
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
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
                paraIntents.add(generateParameterConfigIntent(parameter, specItemDeviation.getStdSpecReq(), specItemDeviation.getOriginalSpecReq()));
            }
        }

        return paraIntents;
    }

    /**
     * 生成数量参数配置意图
     */
    public ParameterConfigIntent generateParameterConfigIntent4Qty(Parameter parameter, Integer reqQty) {
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
     * 根据 Specification 的 type/compare/specValue 和 Parameter 的 type/options 来生成配置意图选项
     * 
     * @param parameter 参数
     * @param stdSpecReq 标准规格需求
     * @param originalSpecReq 原始规格需求
     * @return 参数配置意图
     */
    public ParameterConfigIntent generateParameterConfigIntent(Parameter parameter,
        Specification stdSpecReq, String originalSpecReq) {
        ParameterConfigIntent paraConfigIntent = new ParameterConfigIntent();
        paraConfigIntent.setCode(parameter.getCode());
        paraConfigIntent.setBase(parameter);
        paraConfigIntent.setIntentOptions(new ArrayList<>());

        // 如果参数有选项列表（枚举型参数）
        if (parameter.getOptions() != null && !parameter.getOptions().isEmpty()) {
            // 解析规格需求的值
            double specValue = parseValue(stdSpecReq.getSpecValue());
            String compare = stdSpecReq.getCompare();
            
            // 遍历参数选项，根据比较操作符过滤匹配的选项
            for (ParameterOption paramOption : parameter.getOptions()) {
                // 解析选项的值
                double optionValue = parseValue(paramOption.getValue());
                
                // 根据比较操作符判断是否匹配
                boolean matches = false;
                switch (compare) {
                    case "=":
                        // 等于：选项值等于规格需求值（允许小的误差）
                        matches = Math.abs(optionValue - specValue) < 0.01;
                        break;
                    case ">":
                        // 大于：选项值大于规格需求值
                        matches = optionValue > specValue;
                        break;
                    case ">=":
                        // 大于等于：选项值大于等于规格需求值
                        matches = optionValue >= specValue;
                        break;
                    case "<":
                        // 小于：选项值小于规格需求值
                        matches = optionValue < specValue;
                        break;
                    case "<=":
                        // 小于等于：选项值小于等于规格需求值
                        matches = optionValue <= specValue;
                        break;
                    default:
                        // 未知操作符，默认不匹配
                        log.warn("Unknown compare operator: {}", compare);
                        matches = false;
                        break;
                }
                
                // 如果匹配，则添加配置意图选项
                if (matches) {
                ParameterConfigIntentOption option = new ParameterConfigIntentOption();
                    // 使用 code 作为输出值（如 "48核"），而不是 value（如 48）
                    option.setValue(paramOption.getCode());
                option.setMessage("根据规格需求 " + originalSpecReq + 
                        " 选择配置: " + paramOption.getCode());
                option.setIsVisited(false);
                paraConfigIntent.getIntentOptions().add(option);
                }
            }
        } else {
            // 输入型参数（没有选项列表），使用默认值
            ParameterConfigIntentOption option = new ParameterConfigIntentOption();
            option.setValue(parameter.getDefaultValue());
            option.setMessage("使用默认值: " + parameter.getDefaultValue());
            option.setIsVisited(false);
            paraConfigIntent.getIntentOptions().add(option);
        }

        return paraConfigIntent;
    }

    /**
     * 解析数值
     * 从字符串中提取数字部分并转换为 double
     * 
     * @param value 字符串值
     * @return 解析后的数值
     */
    private double parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        // 提取数字部分（包括负号和小数点）
        String numberStr = value.replaceAll("[^0-9.-]", "");
        if (numberStr.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse value: {}", value, e);
            return 0.0;
        }
    }
}

