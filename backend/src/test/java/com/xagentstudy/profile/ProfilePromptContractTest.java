package com.xagentstudy.profile;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilePromptContractTest {
    @Test
    void profilePromptRequiresAtLeastSixStudentDimensions() throws Exception {
        String prompt = new String(
                getClass().getResourceAsStream("/prompts/profile.md").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(prompt).contains("不少于 6 个画像维度");
        assertThat(prompt).contains("学习目标");
        assertThat(prompt).contains("知识基础");
        assertThat(prompt).contains("学习节奏");
        assertThat(prompt).contains("认知风格");
        assertThat(prompt).contains("易错点");
        assertThat(prompt).contains("资源偏好");
    }
}
