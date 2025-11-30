package com.pcagent.model;

/**
 * 偏离度
 */
public enum DeviationDegree {
    NOT_FOUND("not found"),
    POSITIVE("positive deviation"),
    NEGATIVE("negative deviation"),
    NONE("no deviation");

    private final String description;

    DeviationDegree(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

