package com.pcagent.model;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * 计划
 */
@Data
public class Plan {
    public static final String STEP1 = "step1";
    public static final String STEP2 = "step2";
    public static final String STEP3 = "step3";

    private List<String> tasks = Arrays.asList(STEP1, STEP2, STEP3);
}

