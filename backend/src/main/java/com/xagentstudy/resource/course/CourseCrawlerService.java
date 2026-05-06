package com.xagentstudy.resource.course;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.resource.LearningResourceResponse;
import com.xagentstudy.resource.LearningResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CourseCrawlerService {
    private final CourseResourceCandidateRepository candidateRepository;
    private final CourseCrawlerOutputParser outputParser;
    private final LearningResourceService resourceService;
    private final Path crawlerScript;

    public CourseCrawlerService(
            CourseResourceCandidateRepository candidateRepository,
            CourseCrawlerOutputParser outputParser,
            LearningResourceService resourceService,
            @Value("${app.course-crawler.script:scripts/course_crawler.py}") String crawlerScript
    ) {
        this.candidateRepository = candidateRepository;
        this.outputParser = outputParser;
        this.resourceService = resourceService;
        this.crawlerScript = Path.of(crawlerScript);
    }

    @Transactional(readOnly = true)
    public List<CourseResourceCandidateResponse> list() {
        return candidateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CourseResourceCandidateResponse::from)
                .toList();
    }

    @Transactional
    public List<CourseResourceCandidateResponse> crawl(CrawlCoursesRequest request) {
        List<CourseCrawlerCandidate> crawled = runCrawler(request);
        List<CourseResourceCandidate> saved = crawled.stream()
                .map(candidate -> normalize(candidate, request))
                .map(candidate -> new CourseResourceCandidate(candidate, AuthContext.currentUserId()))
                .map(candidateRepository::save)
                .toList();
        return saved.stream().map(CourseResourceCandidateResponse::from).toList();
    }

    @Transactional
    public LearningResourceResponse publish(Long candidateId) {
        CourseResourceCandidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "课程候选不存在"));
        if ("PUBLISHED".equals(candidate.getStatus())) {
            throw new BusinessException("VALIDATION_ERROR", "课程候选已发布");
        }
        LearningResourceResponse resource = resourceService.createExternalCourse(
                candidate.getTitle(),
                candidate.getDescription(),
                candidate.getCourseUrl(),
                candidate.getSourceName(),
                candidate.getSubjectName(),
                candidate.getSubjectScope(),
                candidate.getTags()
        );
        candidate.publish(resource.id());
        return resource;
    }

    @Transactional
    public CourseResourceCandidateResponse reject(Long candidateId) {
        CourseResourceCandidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "课程候选不存在"));
        candidate.reject();
        return CourseResourceCandidateResponse.from(candidate);
    }

    private List<CourseCrawlerCandidate> runCrawler(CrawlCoursesRequest request) {
        try {
            List<String> command = new ArrayList<>();
            command.add("python3");
            command.add(crawlerScript.toString());
            if (!blank(request.query())) {
                command.add("--query");
                command.add(request.query());
            }
            if (!blank(request.url())) {
                command.add("--url");
                command.add(request.url());
            }
            command.add("--limit");
            command.add(String.valueOf(request.limit() == null ? 8 : Math.min(20, Math.max(1, request.limit()))));
            Process process = new ProcessBuilder(command)
                    .directory(Path.of("").toAbsolutePath().toFile())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(Duration.ofSeconds(45).toMillis(), TimeUnit.MILLISECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("CRAWLER_TIMEOUT", "课程采集超时");
            }
            if (process.exitValue() != 0) {
                throw new BusinessException("CRAWLER_FAILED", output.isBlank() ? "课程采集失败" : output.trim());
            }
            return outputParser.parse(output);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("CRAWLER_FAILED", "无法启动 Python 课程爬虫");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("CRAWLER_FAILED", "课程采集被中断");
        }
    }

    private CourseCrawlerCandidate normalize(CourseCrawlerCandidate candidate, CrawlCoursesRequest request) {
        return new CourseCrawlerCandidate(
                candidate.title(),
                candidate.description(),
                candidate.url(),
                candidate.source(),
                candidate.coverUrl(),
                blank(request.subjectName()) ? candidate.subjectName() : request.subjectName().trim(),
                blank(request.subjectScope()) ? candidate.subjectScope() : request.subjectScope().trim(),
                tags(candidate, request),
                candidate.difficulty()
        );
    }

    private List<String> tags(CourseCrawlerCandidate candidate, CrawlCoursesRequest request) {
        List<String> tags = new ArrayList<>(candidate.tags());
        if (!blank(request.tags())) {
            for (String tag : request.tags().split("[,，]")) {
                String safe = tag.trim();
                if (!safe.isBlank() && !tags.contains(safe)) {
                    tags.add(safe);
                }
            }
        }
        return tags.stream().limit(8).toList();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
