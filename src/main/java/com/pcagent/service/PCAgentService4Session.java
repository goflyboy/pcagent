package com.pcagent.service;

import com.pcagent.exception.InvalidInputException;
import com.pcagent.model.*;
import com.pcagent.util.SessionUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pcagent.util.Pair;

import java.util.List;

/**
 * 产品配置Agent会话服务
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class PCAgentService4Session {
    private final String sessionId;
    private final LLMInvoker llmInvoker;
    private final ProductSpecificationParserService specParserService;
    private final ProductSelectionService selectionService;
    private final ProductConfigService configService;
    
    private Session currentSession;
    private Plan plan = new Plan();

    /**
     * 生成配置
     */
    public void doGeneratorConfig(String userInput) {
        // 创建会话
        currentSession = SessionUtils.create(sessionId, plan);

        try {
            // 调用parseConfigReq解析客户需求
            ConfigReq req = parseConfigReq(userInput);
            validConfigReq(req);
            SessionUtils.updateSession4NextStep(currentSession, req, Plan.STEP1);

            // 解析规格
            List<ProductSpecificationReq> productSpecReqByCatalogNode = specParserService
                    .parseProductSpecs(req.getProductSerial(), req.getSpecReqItems());
            SessionUtils.updateSession4NextStep(currentSession, productSpecReqByCatalogNode, Plan.STEP2);

            // 产品选型
            Pair<List<ProductDeviationDegree>, ProductDeviationDegree> selectionResult = 
                    selectionService.selectProduct(productSpecReqByCatalogNode);
            SessionUtils.updateSession4NextStep(currentSession, selectionResult, Plan.STEP2);

            // 参数配置结果
            if (selectionResult.getSecond() != null) {
                ProductConfig config = configService.doParameterConfigs(
                        selectionResult.getSecond(), req);
                SessionUtils.updateSession4NextStep(currentSession, config, Plan.STEP3);
            }

            // 更新进度为完成
            if (currentSession.getProgress() != null) {
                currentSession.getProgress().setCurrent(plan.getTasks().size());
                currentSession.getProgress().setMessage("配置完成");
            }

        } catch (Exception e) {
            log.error("Failed to generate config", e);
            if (currentSession.getProgress() != null) {
                currentSession.getProgress().setMessage("配置失败: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * 解析配置需求
     */
    ConfigReq parseConfigReq(String input) {
        // 调用LLMInvoker
        String productSerials = "ONU,电脑,服务器,路由器";
        return llmInvoker.parseConfigReq(input, productSerials);
    }

    /**
     * 校验配置需求
     */
    void validConfigReq(ConfigReq req) {
        if (req == null) {
            throw new InvalidInputException("配置需求不能为空");
        }
        if (req.getProductSerial() == null || req.getProductSerial().trim().isEmpty()) {
            throw new InvalidInputException("产品系列不能为空");
        }
        if (req.getTotalQuantity() == null || req.getTotalQuantity() <= 0) {
            throw new InvalidInputException("总套数必须大于0");
        }
    }
}

