package com.pcagent.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 简单的Pair工具类
 * 替代Spring的Pair（可能在某些版本中不可用）
 */
@Data
@AllArgsConstructor
public class Pair<F, S> {
    private F first;
    private S second;

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }
}

