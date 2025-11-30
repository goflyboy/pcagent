package com.pcagent.service;

import com.pcagent.model.ConfigReq;
import com.pcagent.model.ConfigStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * LLM调用服务
 * 支持deepseek和Qinwen两个模型
 * 注意：由于Spring AI依赖问题，当前使用简单解析实现
 */
@Slf4j
@Service
public class LLMInvoker {
    @Value("${spring.ai.provider:openai}")
    private String provider;

    public LLMInvoker() {
        // 暂时不使用ChatClient，使用简单解析
    }

    /**
     * 解析配置需求
     * 
     * @param userInput     用户输入
     * @param productSerials 产品系列列表
     * @return 配置需求
     */
    public ConfigReq parseConfigReq(String userInput, String productSerials) {
        // 当前使用简单解析，后续可以集成LLM
        return parseConfigReqSimple(userInput, productSerials);
    }

    /**
     * 简单解析配置需求（当LLM不可用时使用）
     */
    private ConfigReq parseConfigReqSimple(String userInput, String productSerials) {
        ConfigReq req = new ConfigReq();
        req.setTotalQuantity(1);
        req.setTotalQuantityMemo("LLM不可用，使用简单解析");
        req.setConfigStrategy(ConfigStrategy.PRICE_MIN_PRIORITY);
        req.setSpecReqItems(new java.util.ArrayList<>());

        // 简单提取产品系列
        String[] serials = productSerials.split(",");
        for (String serial : serials) {
            if (userInput.contains(serial.trim())) {
                req.setProductSerial(serial.trim());
                break;
            }
        }

        // 简单提取数量
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*[台套]");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        if (matcher.find()) {
            req.setTotalQuantity(Integer.parseInt(matcher.group(1)));
        }

        // 简单提取规格项（按行分割）
        String[] lines = userInput.split("\n");
        for (String line : lines) {
            if (line.trim().matches(".*[≥>=<≤].*")) {
                req.getSpecReqItems().add(line.trim());
            }
        }

        return req;
    }
}

