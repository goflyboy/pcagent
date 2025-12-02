package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 规格解析结果VO - Step2显示用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecParseResultVO {
    private List<SpecParseItemVO> items = new ArrayList<>();
}

