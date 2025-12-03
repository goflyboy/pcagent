package com.pcagent.service;

import com.pcagent.model.Parameter;
import com.pcagent.model.ParameterConfigIntent;
import com.pcagent.model.Specification;
import com.pcagent.util.ProductOntoUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.pcagent.service.ParameterConfigIntentAssert.assertIntent;

/**
 * ProductConfigService 测试类
 */
class ProductConfigService4ParameterIntentTest {
    private final ProductConfigService configService = new ProductConfigService(null);
    Parameter parameter = null;
    Specification spec = null;

    @BeforeEach
    void setUp() throws IOException {
            // 准备测试数据 - 使用JSON构建Parameter
            String parameterJson = """
                {
                "code": "CPU_CONFIG",
                "type": "ENUMINT",
                "defaultValue": "32核",
                "sortNo": 1,
                "options": [
                    {"code": "32核", "value": "32", "sortNo": 0},
                    {"code": "48核", "value": "48", "sortNo": 1},
                    {"code": "64核", "value": "64", "sortNo": 2}
                ],
                "refSpecCode": "CPU:处理器核心数"
                }
                """;

            // 使用JSON构建Specification
            String specJson = """
                {
                "specName": "CPU:处理器核心数",
                "compare": ">=",
                "specValue": "48",
                "unit": "核",
                "type": "INTEGER"
                }
                """;

            parameter = ProductOntoUtils.fromJsonString(parameterJson.trim(), Parameter.class);
            spec = ProductOntoUtils.fromJsonString(specJson.trim(), Specification.class);

    }
    /**
     * 测试用例1: >= 操作符，规格需求 >= 48核
     * 预期：匹配 48核 和 64核 两个选项
     */
    @Test
    void testGenerateParameterConfigIntent_GreaterEqual() throws IOException { 
        // 执行测试
        ParameterConfigIntent result = configService.generateParameterConfigIntent(parameter, spec, "");

        // 验证结果 - 使用流式断言
        assertIntent(result)
                .codeEqual("CPU_CONFIG")
                .intentOptionsSize(2)
                .intentOption(0)
                    .valueEqual("48核") 
                .and()
                .intentOption(1)
                    .valueEqual("64核");
    }

    /**
     * 测试用例2: = 操作符，规格需求 = 32核
     * 预期：只匹配 32核 一个选项
     */
    @Test
    void testGenerateParameterConfigIntent_Equal() throws IOException {
        spec.setCompare("=");
        spec.setSpecValue("32");
        // 执行测试
        ParameterConfigIntent result = configService.generateParameterConfigIntent(parameter, spec, "");

        // 验证结果 - 使用流式断言
        assertIntent(result)
                .codeEqual("CPU_CONFIG")
                .intentOptionsSize(1)
                .intentOptionByValue("32核")
                    .valueEqual("32核");
    }

    /**
     * 测试用例3: < 操作符，规格需求 < 48核
     * 预期：只匹配 32核 一个选项
     */
    @Test
    void testGenerateParameterConfigIntent_Less() throws IOException { 
        spec.setCompare("<");
        // 执行测试
        ParameterConfigIntent result = configService.generateParameterConfigIntent(parameter, spec, "");

        // 验证结果 - 使用流式断言
        assertIntent(result)
                .codeEqual("CPU_CONFIG")
                .intentOptionsSize(1)
                .intentOptionByValue("32核")
                    .valueEqual("32核");
    }

    //TODO 待补充其它场景
    
}

