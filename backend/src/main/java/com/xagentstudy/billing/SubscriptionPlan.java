package com.xagentstudy.billing;

import java.util.List;

public enum SubscriptionPlan {
    FREE("Free", 0, 100, 1, List.of("1 个轻量学习计划", "基础 Agent 问答", "基础题库与报告")),
    PLUS("Plus", 1900, 1000, 5, List.of("最多 5 个学习计划", "更多 Agent 调用", "完整测验和复习闭环")),
    PRO("Pro", 4900, 5000, 20, List.of("最多 20 个学习计划", "高频 Agent 调用", "更适合重度学习和长期沉淀"));

    private final String displayName;
    private final int monthlyPriceCents;
    private final int monthlyAgentQuota;
    private final int planQuota;
    private final List<String> features;

    SubscriptionPlan(String displayName, int monthlyPriceCents, int monthlyAgentQuota, int planQuota, List<String> features) {
        this.displayName = displayName;
        this.monthlyPriceCents = monthlyPriceCents;
        this.monthlyAgentQuota = monthlyAgentQuota;
        this.planQuota = planQuota;
        this.features = features;
    }

    public String displayName() {
        return displayName;
    }

    public int monthlyPriceCents() {
        return monthlyPriceCents;
    }

    public int monthlyAgentQuota() {
        return monthlyAgentQuota;
    }

    public int storageQuotaMb() {
        return planQuota;
    }

    public int planQuota() {
        return planQuota;
    }

    public List<String> features() {
        return features;
    }

    public boolean higherThan(SubscriptionPlan other) {
        return this.ordinal() > other.ordinal();
    }

    public boolean lowerThan(SubscriptionPlan other) {
        return this.ordinal() < other.ordinal();
    }

    public static SubscriptionPlan fromCode(String code) {
        if (code == null || code.isBlank()) {
            return FREE;
        }
        for (SubscriptionPlan plan : values()) {
            if (plan.name().equalsIgnoreCase(code.trim())) {
                return plan;
            }
        }
        return FREE;
    }
}
