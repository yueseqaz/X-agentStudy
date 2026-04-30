package com.xagentstudy.billing;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "subscription_accounts")
public class SubscriptionAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String planCode;
    private Integer monthlyAgentQuota;
    private Integer usedAgentCalls;
    private Integer storageQuotaMb;
    private Integer usedStorageMb;
    private Integer walletBalanceCents;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private OffsetDateTime updatedAt;

    protected SubscriptionAccount() {
    }

    public SubscriptionAccount(Long userId, String planCode, Integer monthlyAgentQuota, Integer storageQuotaMb) {
        this.userId = userId;
        this.planCode = planCode;
        this.monthlyAgentQuota = monthlyAgentQuota;
        this.usedAgentCalls = 0;
        this.storageQuotaMb = storageQuotaMb;
        this.usedStorageMb = 0;
        this.walletBalanceCents = 0;
        this.periodStart = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public Integer getMonthlyAgentQuota() {
        return monthlyAgentQuota;
    }

    public Integer getUsedAgentCalls() {
        return usedAgentCalls;
    }

    public Integer getStorageQuotaMb() {
        return storageQuotaMb;
    }

    public Integer getUsedStorageMb() {
        return usedStorageMb;
    }

    public Integer getWalletBalanceCents() {
        return walletBalanceCents == null ? 0 : walletBalanceCents;
    }

    public OffsetDateTime getPeriodStart() {
        return periodStart;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void consumeAgentCalls(int amount) {
        this.usedAgentCalls = this.usedAgentCalls + amount;
        this.updatedAt = OffsetDateTime.now();
    }

    public void resetUsage() {
        this.usedAgentCalls = 0;
        this.usedStorageMb = 0;
        this.periodStart = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void changePlan(SubscriptionPlan plan) {
        this.planCode = plan.name();
        this.monthlyAgentQuota = plan.monthlyAgentQuota();
        this.storageQuotaMb = plan.storageQuotaMb();
        this.periodStart = OffsetDateTime.now();
        this.periodEnd = OffsetDateTime.now().plusMonths(1);
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean hasAgentQuota(int amount) {
        return this.usedAgentCalls + amount <= this.monthlyAgentQuota;
    }

    public boolean hasWalletBalance(int amountCents) {
        return getWalletBalanceCents() >= amountCents;
    }

    public void rechargeWallet(int amountCents) {
        this.walletBalanceCents = getWalletBalanceCents() + amountCents;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deductWallet(int amountCents) {
        this.walletBalanceCents = getWalletBalanceCents() - amountCents;
        this.updatedAt = OffsetDateTime.now();
    }
}
