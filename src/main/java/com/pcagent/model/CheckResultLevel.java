package com.pcagent.model;

/**
 * 检查结果级别
 */
public enum CheckResultLevel {
    ERROR(1),
    WARNING(2),
    SUCCESS(0);

    private final int level;

    CheckResultLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

