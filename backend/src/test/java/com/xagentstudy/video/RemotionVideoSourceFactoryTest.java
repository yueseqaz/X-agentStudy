package com.xagentstudy.video;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemotionVideoSourceFactoryTest {
    private final RemotionVideoSourceFactory factory = new RemotionVideoSourceFactory();

    @Test
    void buildsRunnableRemotionSourceFromKnowledgePoint() {
        RemotionVideoDraft draft = factory.create(new KnowledgeVideoContext(
                "Redis 系统学习计划",
                "第 1 章：Redis 基础与数据模型",
                "基础命令单元",
                "Key 命名与过期时间",
                "能设计清晰的 Key 命名，并解释过期时间对数据生命周期的影响",
                "FOUNDATION",
                ""
        ));

        assertThat(draft.title()).contains("Key 命名与过期时间");
        assertThat(draft.scenes()).hasSize(4);
        assertThat(draft.sourceCode())
                .contains("import {AbsoluteFill")
                .contains("export const RemotionKnowledgeVideo")
                .contains("Key 命名与过期时间")
                .contains("Redis 系统学习计划");
    }

    @Test
    void sanitizesTextForTypescriptStringLiterals() {
        RemotionVideoDraft draft = factory.create(new KnowledgeVideoContext(
                "A \"quoted\" plan",
                "Chapter <One>",
                "Unit",
                "JS `template` point",
                "Use backslash \\ safely",
                "ADVANCED",
                ""
        ));

        assertThat(draft.sourceCode())
                .contains("A \\\"quoted\\\" plan")
                .contains("JS \\`template\\` point")
                .contains("Use backslash \\\\ safely");
    }

    @Test
    void extractsScenesFromGeneratedDocumentContent() {
        String markdown = """
                # Key 命名与过期时间

                Redis Key 的命名应该表达业务对象、唯一标识和数据用途。

                ## 为什么要设置过期时间

                过期时间决定数据生命周期，能避免临时缓存长期占用内存。

                - 使用冒号分隔业务层级
                - TTL 要和业务有效期一致
                - 热点 Key 需要避免同一时间集中失效
                """;

        RemotionVideoDraft draft = factory.create(new KnowledgeVideoContext(
                "Redis 系统学习计划",
                "第 1 章：Redis 基础与数据模型",
                "基础命令单元",
                "Key 命名与过期时间",
                "能设计清晰的 Key 命名",
                "FOUNDATION",
                markdown
        ));

        assertThat(draft.scenes())
                .extracting(RemotionScene::title)
                .contains("Key 命名与过期时间", "为什么要设置过期时间");
        assertThat(draft.sourceCode())
                .contains("Redis Key 的命名应该表达业务对象")
                .contains("热点 Key 需要避免同一时间集中失效")
                .doesNotContain("先建立这个知识点在整份计划中的位置。");
    }
}
