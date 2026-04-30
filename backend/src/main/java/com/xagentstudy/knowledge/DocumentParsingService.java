package com.xagentstudy.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.agent.task.AgentTaskService;
import com.xagentstudy.rag.chunk.KnowledgeChunkService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DocumentParsingService {
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkService chunkService;
    private final AgentTaskService taskService;
    private final ObjectMapper objectMapper;

    public DocumentParsingService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkService chunkService,
            AgentTaskService taskService,
            ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.chunkService = chunkService;
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void parseAsync(Long documentId, Long taskId) {
        taskService.markRunning(taskId);
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        try {
            ParseResult result = parse(document, Path.of(document.getStorageKey()));
            document.markParseSuccess(
                    result.summary(),
                    result.sectionsJson(),
                    result.keyPointsJson(),
                    result.knowledgeTreeJson(),
                    result.contentLength()
            );
            taskService.markSuccess(
                    taskId,
                    "{\"documentId\":" + documentId
                            + ",\"parseStatus\":\"PARSE_SUCCESS\""
                            + ",\"chunkCount\":" + result.chunkCount()
                            + ",\"keyPointCount\":" + result.keyPointCount() + "}"
            );
        } catch (Exception ex) {
            document.markParseFailed(ex.getMessage());
            taskService.markFailed(taskId, ex.getMessage());
        }
    }

    @Transactional
    public void parseGeneratedContent(Long documentId, String content) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        try {
            ParseResult result = parseContent(document, content);
            document.markParseSuccess(
                    result.summary(),
                    result.sectionsJson(),
                    result.keyPointsJson(),
                    result.knowledgeTreeJson(),
                    result.contentLength()
            );
        } catch (Exception ex) {
            document.markParseFailed(ex.getMessage());
        }
    }

    private ParseResult parse(KnowledgeDocument document, Path filePath) throws IOException {
        String content = extractContent(document.getType(), filePath);
        return parseContent(document, content);
    }

    private ParseResult parseContent(KnowledgeDocument document, String content) {
        int chunkCount = chunkService.replaceChunks(document.getPlanId(), document.getId(), content);
        List<SectionItem> sections = extractSections(content);
        List<String> keyPoints = extractKeyPoints(content, sections);
        String summary = buildSummary(document, content, chunkCount, sections, keyPoints);
        String sectionsJson = toJson(sections);
        String keyPointsJson = toJson(keyPoints);
        String knowledgeTreeJson = toJson(buildKnowledgeTree(document, sections, keyPoints));
        return new ParseResult(summary, chunkCount, sectionsJson, keyPointsJson, knowledgeTreeJson, content.length(), keyPoints.size());
    }

    private String buildSummary(
            KnowledgeDocument document,
            String content,
            int chunkCount,
            List<SectionItem> sections,
            List<String> keyPoints
    ) {
        String preview = content.replace("\r\n", "\n").replace("\n", " ").trim();
        if (preview.length() > 220) {
            preview = preview.substring(0, 220) + "...";
        }
        String firstPoints = keyPoints.isEmpty() ? "暂无明显知识点" : String.join("、", keyPoints.subList(0, Math.min(5, keyPoints.size())));
        return "已解析 " + document.getName()
                + "，生成 " + chunkCount + " 个知识切片、"
                + sections.size() + " 个章节线索。重点包括：" + firstPoints
                + "。内容预览：" + (preview.isBlank() ? "文件内容为空" : preview);
    }

    private List<SectionItem> extractSections(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n");
        if (normalized.isBlank()) {
            return List.of();
        }
        Pattern headingPattern = Pattern.compile("^(#{1,6}\\s+|第[一二三四五六七八九十百0-9]+[章节讲部分]\\s*|[0-9]+[.、]\\s+|[一二三四五六七八九十]+[、.]\\s*).+");
        List<String> lines = normalized.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        List<SectionItem> sections = new ArrayList<>();
        int order = 1;
        for (String line : lines) {
            String compact = line.replaceAll("\\s+", " ");
            if (compact.length() <= 80 && headingPattern.matcher(compact).find()) {
                sections.add(new SectionItem(order++, cleanHeading(compact), "由文档标题/编号识别"));
            }
            if (sections.size() >= 12) {
                break;
            }
        }
        if (sections.isEmpty()) {
            int blockSize = Math.max(1, lines.size() / 4);
            for (int index = 0; index < lines.size() && sections.size() < 4; index += blockSize) {
                String title = lines.get(index);
                if (title.length() > 48) {
                    title = title.substring(0, 48) + "...";
                }
                sections.add(new SectionItem(sections.size() + 1, title, "由内容段落自动切分"));
            }
        }
        return sections;
    }

    private String cleanHeading(String value) {
        return value.replaceFirst("^#{1,6}\\s+", "").trim();
    }

    private List<String> extractKeyPoints(String content, List<SectionItem> sections) {
        Map<String, Integer> score = new LinkedHashMap<>();
        for (SectionItem section : sections) {
            addPoint(score, section.title(), 4);
        }
        String normalized = content == null ? "" : content.toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[\\s,，。！？?;；:：、（）()\\[\\]{}<>《》\"'`]+")) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 32 && !isStopWord(trimmed)) {
                addPoint(score, trimmed, trimmed.matches(".*[a-zA-Z].*") ? 2 : 1);
            }
        }
        return score.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(12)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void addPoint(Map<String, Integer> score, String raw, int weight) {
        String point = raw == null ? "" : raw.replaceAll("^[0-9一二三四五六七八九十百#、.\\s]+", "").trim();
        if (!point.isBlank() && point.length() <= 48 && !isStopWord(point.toLowerCase(Locale.ROOT))) {
            score.merge(point, weight, Integer::sum);
        }
    }

    private boolean isStopWord(String value) {
        return List.of("the", "and", "for", "with", "this", "that", "from", "你", "我", "他", "它", "以及", "或者", "一个", "可以", "进行", "通过", "如果", "需要", "当前")
                .contains(value);
    }

    private Map<String, Object> buildKnowledgeTree(KnowledgeDocument document, List<SectionItem> sections, List<String> keyPoints) {
        return Map.of(
                "document", document.getName(),
                "root", "计划资料知识树",
                "sections", sections,
                "keyPoints", keyPoints,
                "reviewSeeds", keyPoints.stream().limit(6).toList()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String extractContent(String type, Path filePath) throws IOException {
        return switch (type) {
            case "TXT", "MD", "MARKDOWN" -> Files.readString(filePath, StandardCharsets.UTF_8);
            case "PDF" -> extractPdf(filePath);
            case "DOCX" -> extractDocx(filePath);
            case "PPTX" -> extractPptx(filePath);
            case "PPT" -> extractPpt(filePath);
            default -> throw new IOException("Unsupported document type: " + type);
        };
    }

    private String extractPdf(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(Path filePath) throws IOException {
        try (InputStream input = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(input)) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(builder, paragraph.getText());
            }
            return builder.toString();
        }
    }

    private String extractPptx(Path filePath) throws IOException {
        try (InputStream input = Files.newInputStream(filePath);
             XMLSlideShow slideShow = new XMLSlideShow(input)) {
            StringBuilder builder = new StringBuilder();
            slideShow.getSlides().forEach(slide -> {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        appendLine(builder, textShape.getText());
                    }
                }
            });
            return builder.toString();
        }
    }

    private String extractPpt(Path filePath) throws IOException {
        try (InputStream input = Files.newInputStream(filePath);
             HSLFSlideShow slideShow = new HSLFSlideShow(input)) {
            StringBuilder builder = new StringBuilder();
            slideShow.getSlides().forEach(slide -> {
                List<List<HSLFTextParagraph>> paragraphs = slide.getTextParagraphs();
                for (List<HSLFTextParagraph> group : paragraphs) {
                    appendLine(builder, HSLFTextParagraph.getRawText(group));
                }
            });
            return builder.toString();
        }
    }

    private void appendLine(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value.trim()).append('\n');
        }
    }

    private record SectionItem(int order, String title, String source) {
    }

    private record ParseResult(
            String summary,
            int chunkCount,
            String sectionsJson,
            String keyPointsJson,
            String knowledgeTreeJson,
            int contentLength,
            int keyPointCount
    ) {
    }
}
