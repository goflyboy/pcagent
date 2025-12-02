package com.pcagent.service;

import com.pcagent.exception.InvalidInputException;
import com.pcagent.model.ConfigReq;
import com.pcagent.util.StringHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 配置需求识别服务
 * 负责解析和校验配置需求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigReqRecognitionService {
    private final LLMInvoker llmInvoker;

    /**
     * 解析配置需求
     * 
     * @param input 用户输入
     * @param productSerials 产品系列列表（逗号分隔的字符串）
     * @return 配置需求
     */
    public ConfigReq parseConfigReq(String input) {
       
        String productSerials = "ONU,电脑,服务器,路由器";
        // 调用LLMInvoker
        ConfigReq req = llmInvoker.parseConfigReq(input, productSerials);
        
        // 规范化 specReqItems：去掉空格，替换特殊字符
        if (req != null && req.getSpecReqItems() != null && !req.getSpecReqItems().isEmpty()) {
            List<String> normalizedItems = StringHelper.normalizeSpecReqItems(req.getSpecReqItems());
            req.setSpecReqItems(normalizedItems);
            log.debug("Normalized specReqItems: {} items", normalizedItems.size());
        }
        
        return req;
    }

    /**
     * 校验配置需求
     * 
     * @param req 配置需求
     * @throws InvalidInputException 如果配置需求无效
     */
    public void validConfigReq(ConfigReq req) {
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

