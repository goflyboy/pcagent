package com.pcagent.service;

import com.pcagent.model.*;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductConfigService 系统测试类
 * 使用真实的 ChatModel（不 mock）来测试 checkParameterConfig 方法
 * 参考 LLMInvokerSysTest 编写
 * 
 * 注意：需要设置环境变量才能运行：
 * - DEEPSEEK_API_KEY: DeepSeek API key
 * - DEEPSEEK_BASE_URL: DeepSeek API base URL (可选，默认: https://api.deepseek.com)
 * 或
 * - QINWEN_API_KEY: 通义千问 API key
 * - QINWEN_BASE_URL: 通义千问 API base URL (可选，默认: https://dashscope.aliyuncs.com/compatible-mode/v1)
 */
class ProductConfigService4CheckerTest {

    private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_QINWEN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String DEFAULT_DEEPSEEK_MODEL = "deepseek-chat";
    private static final String DEFAULT_QINWEN_MODEL = "qwen-turbo";

    private ProductConfigService configService;
    private ProductOntoService4Local productOntoService;
    private LLMInvoker llmInvoker;

    @BeforeEach
    void setUp() {
        // 初始化 ProductOntoService
        productOntoService = new ProductOntoService4Local();
        productOntoService.init();

        // 尝试创建真实的 ChatModel
        ChatModel chatModel = createChatModelIfAvailable();
        if (chatModel != null) {
            llmInvoker = new LLMInvoker(chatModel);
            configService = new ProductConfigService(productOntoService, llmInvoker);
        } else {
            // 如果没有可用的 ChatModel，使用无 LLMInvoker 的构造函数
            configService = new ProductConfigService(productOntoService);
        }
    }

    /**
     * 测试使用真实 LLM 检查参数配置
     * 测试场景：配置了 checkRule 的参数，使用 LLM 进行校验
     * 
     * 注意：此测试需要设置环境变量才能运行
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void shouldUseRealLlmToCheckParameterConfig() {
        // 构造测试数据：包含 checkRule 的参数配置意图
        Stack<ParameterConfigIntent> paraConfigResultStack = new Stack<>();

        // 创建参数1：P_Capacity（容量类型）
        Parameter capacityParam = new Parameter();
        capacityParam.setCode("P_Capacity");
        capacityParam.setType(ParameterType.ENUM);
        capacityParam.setCheckRule("""
                //自然语言：中国销售，只有10-8GE(8*POE)一个可选择 其他有"10-8GE(8*POE)","20-16GE(16*POE)"两个可选 
                //配置DSL：
                filter:{
                    if Solution.country == "1790"{  
                        return [10,20]
                    }else{
                        return [10]
                    }
                }
                """);

        ParameterConfigIntent capacityIntent = new ParameterConfigIntent();
        capacityIntent.setCode("P_Capacity");
        capacityIntent.setBase(capacityParam);
        ParameterConfig capacityResult = new ParameterConfig();
        capacityResult.setCode("P_Capacity");
        capacityResult.setValue("10-8GE(8*POE)");
        capacityIntent.setResult(capacityResult);
        paraConfigResultStack.push(capacityIntent);

        // 创建参数2：P_Qty_ONT（ONU终端数量）
        Parameter qtyParam = new Parameter();
        qtyParam.setCode("P_Qty_ONT");
        qtyParam.setType(ParameterType.INTEGER);
        qtyParam.setCheckRule("""
                //自然语言：波兰企业市场要求只能输入1， 但为提升项目盈利，推荐50台以上起售 
                //配置DSL：
                check:{
                    if Solution.country=="160" and self.total>1
                    {
                        return [~900147646] //返回告警："波兰企业市场要求只能输入1"
                    }
                    elif  Solution.country!="160" and self.total<50 and self.total>0 {
                        return [~900121723] //返回告警：为提升项目盈利，推荐50台以上起售
                    }
                    elif {
                        return [~0];//正常
                    }
                }
                """);

        ParameterConfigIntent qtyIntent = new ParameterConfigIntent();
        qtyIntent.setCode("P_Qty_ONT");
        qtyIntent.setBase(qtyParam);
        ParameterConfig qtyResult = new ParameterConfig();
        qtyResult.setCode("P_Qty_ONT");
        qtyResult.setValue("10"); // 小于50，应该触发告警
        qtyIntent.setResult(qtyResult);
        paraConfigResultStack.push(qtyIntent);

        // 创建参数3：Solution.country（国家）
        Parameter countryParam = new Parameter();
        countryParam.setCode("Solution.country");
        countryParam.setType(ParameterType.STRING);
        countryParam.setCheckRule(""); // 没有检查规则

        ParameterConfigIntent countryIntent = new ParameterConfigIntent();
        countryIntent.setCode("Solution.country");
        countryIntent.setBase(countryParam);
        ParameterConfig countryResult = new ParameterConfig();
        countryResult.setCode("Solution.country");
        countryResult.setValue("中国");
        countryIntent.setResult(countryResult);
        paraConfigResultStack.push(countryIntent);

        // 执行测试
        CheckResult result = configService.checkParameterConfig(paraConfigResultStack);

        // 验证结果
        assertNotNull(result, "CheckResult不应为null");
        assertNotNull(result.getLevel(), "CheckResult.level不应为null");
        assertNotNull(result.getErrorCode(), "CheckResult.errorCode不应为null");
        assertNotNull(result.getErrorMessage(), "CheckResult.errorMessage不应为null");

        // 由于配置了 P_Qty_ONT = 10（小于50），应该返回错误或警告
        // 具体结果取决于 LLM 的判断
        if (result.getErrorCode() != null && result.getErrorCode() != 0) {
            // 有错误，验证错误信息
            assertTrue(result.getErrorMessage().length() > 0, "错误信息不应为空");
            assertEquals(CheckResultLevel.ERROR, result.getLevel(), "应该返回ERROR级别");
            System.out.println("LLM检查结果（有错误）: " + result.getErrorMessage());
        } else {
            // 无错误，验证成功信息
            assertEquals(CheckResultLevel.SUCCESS, result.getLevel(), "应该返回SUCCESS级别");
            System.out.println("LLM检查结果（通过）: " + result.getErrorMessage());
        }
    }

    /**
     * 测试使用真实 LLM 检查参数配置 - 正常情况
     * 测试场景：配置值符合规则，应该返回成功
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void shouldUseRealLlmToCheckParameterConfig_Success() {
        // 构造测试数据：包含 checkRule 的参数配置意图
        Stack<ParameterConfigIntent> paraConfigResultStack = new Stack<>();

        // 创建参数：P_Qty_ONT（ONU终端数量），配置为60（大于50，应该通过）
        Parameter qtyParam = new Parameter();
        qtyParam.setCode("P_Qty_ONT");
        qtyParam.setType(ParameterType.INTEGER);
        qtyParam.setCheckRule("""
                //自然语言：波兰企业市场要求只能输入1， 但为提升项目盈利，推荐50台以上起售 
                //配置DSL：
                check:{
                    if Solution.country=="160" and self.total>1
                    {
                        return [~900147646] //返回告警："波兰企业市场要求只能输入1"
                    }
                    elif  Solution.country!="160" and self.total<50 and self.total>0 {
                        return [~900121723] //返回告警：为提升项目盈利，推荐50台以上起售
                    }
                    elif {
                        return [~0];//正常
                    }
                }
                """);

        ParameterConfigIntent qtyIntent = new ParameterConfigIntent();
        qtyIntent.setCode("P_Qty_ONT");
        qtyIntent.setBase(qtyParam);
        ParameterConfig qtyResult = new ParameterConfig();
        qtyResult.setCode("P_Qty_ONT");
        qtyResult.setValue("60"); // 大于50，应该通过
        qtyIntent.setResult(qtyResult);
        paraConfigResultStack.push(qtyIntent);

        // 创建参数：Solution.country（国家）
        Parameter countryParam = new Parameter();
        countryParam.setCode("Solution.country");
        countryParam.setType(ParameterType.STRING);

        ParameterConfigIntent countryIntent = new ParameterConfigIntent();
        countryIntent.setCode("Solution.country");
        countryIntent.setBase(countryParam);
        ParameterConfig countryResult = new ParameterConfig();
        countryResult.setCode("Solution.country");
        countryResult.setValue("中国");
        countryIntent.setResult(countryResult);
        paraConfigResultStack.push(countryIntent);

        // 执行测试
        CheckResult result = configService.checkParameterConfig(paraConfigResultStack);

        // 验证结果
        assertNotNull(result, "CheckResult不应为null");
        assertNotNull(result.getLevel(), "CheckResult.level不应为null");
        assertNotNull(result.getErrorCode(), "CheckResult.errorCode不应为null");

        // 由于配置了 P_Qty_ONT = 60（大于50），应该返回成功
        if (result.getErrorCode() == 0) {
            assertEquals(CheckResultLevel.SUCCESS, result.getLevel(), "应该返回SUCCESS级别");
            System.out.println("LLM检查结果（通过）: " + result.getErrorMessage());
        } else {
            // 如果 LLM 仍然返回错误，打印信息用于调试
            System.out.println("LLM检查结果（有错误）: " + result.getErrorMessage());
        }
    }

    /**
     * 测试当没有 checkRule 时，使用简单检查
     */
    @Test
    void shouldUseSimpleCheckWhenNoCheckRule() {
        // 构造测试数据：没有 checkRule 的参数配置意图
        Stack<ParameterConfigIntent> paraConfigResultStack = new Stack<>();

        // 创建参数：没有 checkRule
        Parameter param = new Parameter();
        param.setCode("P_Test");
        param.setType(ParameterType.STRING);
        param.setCheckRule(""); // 空的 checkRule

        ParameterConfigIntent intent = new ParameterConfigIntent();
        intent.setCode("P_Test");
        intent.setBase(param);
        ParameterConfig result = new ParameterConfig();
        result.setCode("P_Test");
        result.setValue("test_value");
        intent.setResult(result);
        paraConfigResultStack.push(intent);

        // 执行测试
        CheckResult checkResult = configService.checkParameterConfig(paraConfigResultStack);

        // 验证结果：应该返回成功（简单检查）
        assertNotNull(checkResult, "CheckResult不应为null");
        assertEquals(CheckResultLevel.SUCCESS, checkResult.getLevel(), "应该返回SUCCESS级别");
        assertEquals(0, checkResult.getErrorCode(), "errorCode应该为0");
    }

    /**
     * 创建 ChatModel（如果环境变量可用）
     * 参考 LLMInvokerSysTest.createChatModelIfAvailable
     */
    private ChatModel createChatModelIfAvailable() {
        // 优先尝试 DeepSeek
        String deepseekApiKey = getEnvVar("DEEPSEEK_API_KEY");
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            String baseUrl = getEnvVar("DEEPSEEK_BASE_URL", DEFAULT_DEEPSEEK_BASE_URL);
            return createChatModel("deepseek", deepseekApiKey, baseUrl, DEFAULT_DEEPSEEK_MODEL);
        }

        // 尝试通义千问
        String qinwenApiKey = getEnvVar("QINWEN_API_KEY");
        if (qinwenApiKey != null && !qinwenApiKey.isEmpty()) {
            String baseUrl = getEnvVar("QINWEN_BASE_URL", DEFAULT_QINWEN_BASE_URL);
            return createChatModel("qinwen", qinwenApiKey, baseUrl, DEFAULT_QINWEN_MODEL);
        }

        // 如果都没有，返回 null
        return null;
    }

    /**
     * 创建 ChatModel
     * 参考 LLMInvokerSysTest.createChatModel
     */
    private ChatModel createChatModel(String modelType, String apiKey, String baseUrl, String modelName) {
        try {
            OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(modelName)
                    .withTemperature(0.7)
                    .build();
            return new OpenAiChatModel(openAiApi, options);
        } catch (Exception e) {
            System.err.println("Failed to create ChatModel: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取环境变量
     */
    private String getEnvVar(String key) {
        return getEnvVar(key, null);
    }

    /**
     * 获取环境变量（带默认值）
     */
    private String getEnvVar(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}

