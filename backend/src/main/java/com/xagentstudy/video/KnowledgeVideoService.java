package com.xagentstudy.video;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class KnowledgeVideoService {
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final RemotionVideoSourceFactory sourceFactory;
    private final Path uploadRoot;
    private final Path rendererRoot;

    public KnowledgeVideoService(
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            RemotionVideoSourceFactory sourceFactory,
            @Value("${app.storage.upload-root:uploads}") String uploadRoot,
            @Value("${app.video.renderer-root:remotion-renderer}") String rendererRoot
    ) {
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.sourceFactory = sourceFactory;
        this.uploadRoot = Path.of(uploadRoot);
        this.rendererRoot = Path.of(rendererRoot);
    }

    @Transactional(readOnly = true)
    public KnowledgeVideoResponse generate(Long planId, GenerateKnowledgeVideoRequest request) {
        LearningPlan plan = ensurePlan(planId);
        KnowledgeVideoContext context = new KnowledgeVideoContext(
                plan.getTitle(),
                blankToDefault(request.chapterName(), "当前章节"),
                blankToDefault(request.unitName(), "当前单元"),
                blankToDefault(request.title(), "知识点"),
                blankToDefault(request.outcome(), "完成该知识点学习"),
                blankToDefault(request.level(), "FOUNDATION"),
                blankToDefault(request.documentContent(), "")
        );
        RemotionVideoDraft draft = sourceFactory.create(context);
        String videoId = videoId(planId, request.knowledgePointId(), request.title());
        Path videoDir = videoDir(planId, videoId);
        Path sourceFile = videoDir.resolve("KnowledgeVideo.tsx");
        Path outputFile = videoDir.resolve("knowledge-video.mp4");
        try {
            Files.createDirectories(videoDir.resolve("src"));
            Files.writeString(sourceFile, draft.sourceCode(), StandardCharsets.UTF_8);
            Files.writeString(videoDir.resolve("scenes.json"), scenesJson(draft.scenes()), StandardCharsets.UTF_8);
            RenderResult result = render(videoDir, sourceFile, outputFile);
            return new KnowledgeVideoResponse(
                    videoId,
                    draft.title(),
                    result.ok() ? "READY" : "FAILED",
                    result.message(),
                    draft.sourceCode(),
                    result.ok() ? "/api/v1/plans/" + planId + "/knowledge-videos/" + videoId + "/file" : null,
                    draft.scenes()
            );
        } catch (IOException ex) {
            throw new BusinessException("VIDEO_GENERATION_FAILED", "视频文件写入失败");
        }
    }

    @Transactional(readOnly = true)
    public Resource videoFile(Long planId, String videoId) {
        ensurePlan(planId);
        Path file = videoDir(planId, videoId).resolve("knowledge-video.mp4").normalize();
        if (!file.startsWith(videoRoot(planId)) || !Files.exists(file)) {
            throw new BusinessException("RESOURCE_NOT_FOUND", "视频文件不存在");
        }
        return new FileSystemResource(file);
    }

    private RenderResult render(Path videoDir, Path sourceFile, Path outputFile) {
        Path renderer = rendererRoot.toAbsolutePath().normalize();
        Path script = renderer.resolve("render.mjs");
        if (!Files.exists(script)) {
            return new RenderResult(false, "Remotion 渲染器未安装，请检查 backend/remotion-renderer");
        }
        try {
            Process process = new ProcessBuilder(
                    "node",
                    script.toString(),
                    sourceFile.toAbsolutePath().toString(),
                    outputFile.toAbsolutePath().toString()
            )
                    .directory(renderer.toFile())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(Duration.ofMinutes(3).toMillis(), TimeUnit.MILLISECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                return new RenderResult(false, "视频渲染超时");
            }
            if (process.exitValue() != 0 || !Files.exists(outputFile)) {
                return new RenderResult(false, output.isBlank() ? "视频渲染失败" : trimMessage(output));
            }
            return new RenderResult(true, "视频已生成");
        } catch (IOException ex) {
            return new RenderResult(false, "无法启动 Remotion 渲染器");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RenderResult(false, "视频渲染被中断");
        }
    }

    private LearningPlan ensurePlan(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        directionRepository.findById(plan.getDirectionId())
                .filter(direction -> direction.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return plan;
    }

    private Path videoRoot(Long planId) {
        return uploadRoot.resolve("plans").resolve(String.valueOf(planId)).resolve("videos").normalize();
    }

    private Path videoDir(Long planId, String videoId) {
        return videoRoot(planId).resolve(videoId).normalize();
    }

    private String videoId(Long planId, String pointId, String title) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((planId + ":" + blankToDefault(pointId, title)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception ex) {
            return String.valueOf(Math.abs((planId + ":" + title).hashCode()));
        }
    }

    private String scenesJson(List<RemotionScene> scenes) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < scenes.size(); index++) {
            RemotionScene scene = scenes.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"title\":\"").append(escapeJson(scene.title()))
                    .append("\",\"subtitle\":\"").append(escapeJson(scene.subtitle()))
                    .append("\",\"body\":\"").append(escapeJson(scene.body()))
                    .append("\",\"accent\":\"").append(escapeJson(scene.accent()))
                    .append("\"}");
        }
        return builder.append(']').toString();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimMessage(String output) {
        String compact = output.replaceAll("\\s+", " ").trim();
        return compact.length() > 360 ? compact.substring(0, 360) + "..." : compact;
    }

    private record RenderResult(boolean ok, String message) {
    }
}
