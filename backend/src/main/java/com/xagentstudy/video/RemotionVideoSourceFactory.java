package com.xagentstudy.video;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RemotionVideoSourceFactory {
    public RemotionVideoDraft create(KnowledgeVideoContext context) {
        String point = blankToDefault(context.pointTitle(), "知识点");
        String title = point + " · Remotion 讲解视频";
        List<RemotionScene> scenes = scenesFromDocument(context);
        if (scenes.isEmpty()) {
            scenes = fallbackScenes(context);
        }
        return new RemotionVideoDraft(title, source(title, context, scenes), scenes);
    }

    private List<RemotionScene> fallbackScenes(KnowledgeVideoContext context) {
        String point = blankToDefault(context.pointTitle(), "知识点");
        return List.of(
                new RemotionScene(point, blankToDefault(context.planTitle(), "学习计划"), "先建立这个知识点在整份计划中的位置。", "01"),
                new RemotionScene("学习场景", blankToDefault(context.chapterName(), "当前章节"), "它服务于「" + blankToDefault(context.outcome(), "完成该知识点学习") + "」。", "02"),
                new RemotionScene("关键理解", blankToDefault(context.unitName(), "当前单元"), "用一个真实问题解释概念、边界和常见误区。", "03"),
                new RemotionScene("行动检查", blankToDefault(context.level(), "FOUNDATION"), "最后用 3 个检查点确认自己是否真正掌握。", "04")
        );
    }

    private List<RemotionScene> scenesFromDocument(KnowledgeVideoContext context) {
        String content = context.documentContent();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<DocumentBlock> blocks = documentBlocks(content);
        if (blocks.isEmpty()) {
            return List.of();
        }
        List<RemotionScene> scenes = new ArrayList<>();
        for (DocumentBlock block : blocks) {
            if (scenes.size() == 4) {
                break;
            }
            scenes.add(new RemotionScene(
                    block.title(),
                    scenes.isEmpty() ? blankToDefault(context.planTitle(), "学习计划") : blankToDefault(context.chapterName(), "当前章节"),
                    block.body(),
                    "0" + (scenes.size() + 1)
            ));
        }
        return scenes;
    }

    private List<DocumentBlock> documentBlocks(String markdown) {
        List<DocumentBlock> blocks = new ArrayList<>();
        String currentTitle = "";
        List<String> currentLines = new ArrayList<>();
        for (String rawLine : markdown.split("\\R")) {
            String line = normalizeMarkdownLine(rawLine);
            if (line.isBlank()) {
                continue;
            }
            if (isHeading(rawLine)) {
                addBlock(blocks, currentTitle, currentLines);
                currentTitle = normalizeMarkdownLine(rawLine.replaceFirst("^#{1,6}\\s*", ""));
                currentLines = new ArrayList<>();
                continue;
            }
            currentLines.add(line);
        }
        addBlock(blocks, currentTitle, currentLines);
        return blocks.stream()
                .filter(block -> !block.title().isBlank() && !block.body().isBlank())
                .limit(8)
                .toList();
    }

    private void addBlock(List<DocumentBlock> blocks, String title, List<String> lines) {
        String safeTitle = title == null || title.isBlank() ? "" : title.trim();
        String body = lines.stream()
                .filter(line -> !line.isBlank())
                .limit(4)
                .reduce((left, right) -> left + " · " + right)
                .orElse("");
        if (!safeTitle.isBlank() && !body.isBlank()) {
            blocks.add(new DocumentBlock(safeTitle, trimForVideo(body, 96)));
        }
    }

    private boolean isHeading(String line) {
        return line != null && line.matches("^#{1,6}\\s+.+");
    }

    private String normalizeMarkdownLine(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceFirst("^\\s*[-*+]\\s+", "")
                .replaceFirst("^\\s*\\d+[.)]\\s+", "")
                .replace("**", "")
                .replace("`", "")
                .replaceAll("<[^>]+>", "")
                .trim();
    }

    private String trimForVideo(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private String source(String title, KnowledgeVideoContext context, List<RemotionScene> scenes) {
        return """
                import React from 'react';
                import {AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';

                const scenes = [
                __SCENES__
                ];

                const palette = {
                  ink: '#101010',
                  paper: '#f8f8f4',
                  muted: '#6f6f68',
                  line: '#d9d9d0',
                  inverse: '#ffffff',
                };

                function SceneCard({scene, index}) {
                  const frame = useCurrentFrame();
                  const {fps} = useVideoConfig();
                  const localFrame = frame - index * 90;
                  const progress = spring({frame: Math.max(0, localFrame), fps, config: {damping: 18, stiffness: 90}});
                  const opacity = interpolate(localFrame, [-12, 16, 78, 92], [0, 1, 1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
                  const y = interpolate(progress, [0, 1], [36, 0]);
                  return (
                    <AbsoluteFill style={{opacity, transform: `translateY(${y}px)`, padding: 72, justifyContent: 'center'}}>
                      <div style={{
                        display: 'grid',
                        gap: 26,
                        padding: 56,
                        border: `2px solid ${palette.ink}`,
                        borderRadius: 18,
                        background: index % 2 === 0 ? palette.paper : palette.ink,
                        color: index % 2 === 0 ? palette.ink : palette.inverse,
                        boxShadow: '0 34px 80px rgba(0,0,0,0.18)'
                      }}>
                        <span style={{fontSize: 28, letterSpacing: 8, color: index % 2 === 0 ? palette.muted : '#bdbdb6'}}>{scene.accent}</span>
                        <h1 style={{margin: 0, fontSize: 70, lineHeight: 1.05, fontWeight: 900}}>{scene.title}</h1>
                        <h2 style={{margin: 0, fontSize: 34, color: index % 2 === 0 ? palette.muted : '#d8d8d2'}}>{scene.subtitle}</h2>
                        <p style={{margin: 0, maxWidth: 900, fontSize: 38, lineHeight: 1.45}}>{scene.body}</p>
                      </div>
                    </AbsoluteFill>
                  );
                }

                export const RemotionKnowledgeVideo = () => {
                  const frame = useCurrentFrame();
                  const sweep = interpolate(frame, [0, 360], [-280, 1280], {extrapolateRight: 'clamp'});
                  return (
                    <AbsoluteFill style={{background: palette.paper, fontFamily: 'Avenir Next, PingFang SC, sans-serif', overflow: 'hidden'}}>
                      <div style={{position: 'absolute', inset: 0, opacity: 0.34, backgroundImage: `linear-gradient(${palette.line} 1px, transparent 1px), linear-gradient(90deg, ${palette.line} 1px, transparent 1px)`, backgroundSize: '42px 42px'}} />
                      <div style={{position: 'absolute', top: 0, bottom: 0, left: sweep, width: 220, background: 'rgba(16,16,16,0.08)', transform: 'skewX(-12deg)'}} />
                      <div style={{position: 'absolute', left: 72, top: 42, fontSize: 24, letterSpacing: 5, color: palette.muted}}>X-AgentStudy / __PLAN_TITLE__</div>
                      {scenes.map((scene, index) => <SceneCard key={scene.title} scene={scene} index={index} />)}
                    </AbsoluteFill>
                  );
                };
                """
                .replace("__SCENES__", sceneArray(scenes))
                .replace("__PLAN_TITLE__", escapeTs(blankToDefault(context.planTitle(), title)));
    }

    private String sceneArray(List<RemotionScene> scenes) {
        return scenes.stream()
                .map(scene -> "  {title: \"" + escapeTs(scene.title()) + "\", subtitle: \"" + escapeTs(scene.subtitle()) + "\", body: \"" + escapeTs(scene.body()) + "\", accent: \"" + escapeTs(scene.accent()) + "\"}")
                .reduce((left, right) -> left + ",\n" + right)
                .orElse("");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String escapeTs(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }

    private record DocumentBlock(String title, String body) {
    }
}
