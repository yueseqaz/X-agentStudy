package com.xagentstudy.admin;

public record AdminSetUserDisabledRequest(
        Boolean disabled,
        java.time.OffsetDateTime disabledUntil
) {
}
