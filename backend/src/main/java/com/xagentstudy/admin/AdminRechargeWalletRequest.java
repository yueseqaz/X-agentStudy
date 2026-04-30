package com.xagentstudy.admin;

import jakarta.validation.constraints.Min;

public record AdminRechargeWalletRequest(
        @Min(1) Integer amountCents,
        String remark
) {
}
