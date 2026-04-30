package com.xagentstudy.resource;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LearningResourceService {
    private final LearningResourceRepository resourceRepository;
    private final LearningPlanRepository planRepository;
    private final LearningDirectionRepository directionRepository;
    private final Path uploadRoot;

    public LearningResourceService(
            LearningResourceRepository resourceRepository,
            LearningPlanRepository planRepository,
            LearningDirectionRepository directionRepository,
            @Value("${app.storage.upload-root:uploads}") String uploadRoot
    ) {
        this.resourceRepository = resourceRepository;
        this.planRepository = planRepository;
        this.directionRepository = directionRepository;
        this.uploadRoot = Path.of(uploadRoot);
    }

    @Transactional(readOnly = true)
    public List<LearningResourceResponse> list() {
        return resourceRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(LearningResourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningResourceResponse> recommended(Long planId) {
        LearningPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning plan not found"));
        LearningDirection direction = directionRepository.findById(plan.getDirectionId())
                .filter(item -> item.getUserId().equals(AuthContext.currentUserId()))
                .orElseThrow(() -> new BusinessException("FORBIDDEN", "No access to this plan"));
        return resourceRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(resource -> ResourceTagMatcher.matches(direction.getName(), direction.getCategory(), resource))
                .map(LearningResourceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningResourceResponse detail(Long resourceId) {
        return LearningResourceResponse.from(find(resourceId));
    }

    @Transactional
    public LearningResourceResponse upload(
            MultipartFile file,
            String title,
            String description,
            String subjectName,
            String subjectScope,
            String tags
    ) {
        if (file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "Uploaded file is empty");
        }
        String originalName = file.getOriginalFilename() == null ? "untitled" : file.getOriginalFilename();
        String type = detectResourceType(originalName, file.getContentType());
        if (isBlank(subjectName) || isBlank(subjectScope) || isBlank(tags)) {
            throw new BusinessException("VALIDATION_ERROR", "Subject name, subject scope and tags are required");
        }
        try {
            Path resourceDir = uploadRoot.resolve("resources");
            Files.createDirectories(resourceDir);
            String storedName = UUID.randomUUID() + "-" + sanitizeName(originalName);
            Path target = resourceDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            LearningResource saved = resourceRepository.save(new LearningResource(
                    AuthContext.currentUserId(),
                    isBlank(title) ? originalName : title.trim(),
                    blankToNull(description),
                    type,
                    originalName,
                    file.getContentType(),
                    target.toString(),
                    subjectName.trim(),
                    subjectScope.trim(),
                    tags.trim(),
                    file.getSize()
            ));
            return LearningResourceResponse.from(saved);
        } catch (IOException ex) {
            throw new BusinessException("RESOURCE_UPLOAD_FAILED", "Failed to store uploaded resource");
        }
    }

    @Transactional
    public void delete(Long resourceId) {
        LearningResource resource = find(resourceId);
        resourceRepository.delete(resource);
        try {
            Files.deleteIfExists(Path.of(resource.getStorageKey()));
        } catch (IOException ignored) {
            // Metadata deletion should not fail if the local file is already gone.
        }
    }

    @Transactional(readOnly = true)
    public LearningResourcePreviewResponse preview(Long resourceId) {
        LearningResource resource = find(resourceId);
        if (!"DOCUMENT".equals(resource.getResourceType())) {
            return new LearningResourcePreviewResponse(LearningResourceResponse.from(resource), resource.getResourceType(), "");
        }
        String extension = extension(resource.getOriginalFilename());
        if ("PDF".equals(extension)) {
            return new LearningResourcePreviewResponse(LearningResourceResponse.from(resource), "PDF", "");
        }
        try {
            String content = extractText(extension, Path.of(resource.getStorageKey()));
            return new LearningResourcePreviewResponse(LearningResourceResponse.from(resource), "TEXT", content);
        } catch (IOException ex) {
            throw new BusinessException("RESOURCE_PREVIEW_FAILED", "Unable to preview this document");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> file(Long resourceId) {
        LearningResource resource = find(resourceId);
        FileSystemResource file = new FileSystemResource(Path.of(resource.getStorageKey()));
        if (!file.exists()) {
            throw new BusinessException("RESOURCE_NOT_FOUND", "Resource file not found");
        }
        MediaType mediaType = resource.getContentType() == null || resource.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(resource.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(file);
    }

    private LearningResource find(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Learning resource not found"));
    }

    private String detectResourceType(String filename, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String extension = extension(filename);
        if (type.startsWith("video/") || List.of("MP4", "WEBM", "MOV").contains(extension)) {
            return "VIDEO";
        }
        if (type.startsWith("audio/") || List.of("MP3", "WAV", "M4A", "AAC", "OGG").contains(extension)) {
            return "AUDIO";
        }
        if (type.startsWith("image/") || List.of("PNG", "JPG", "JPEG", "GIF", "WEBP").contains(extension)) {
            return "IMAGE";
        }
        if (List.of("PDF", "DOCX", "PPT", "PPTX", "MD", "MARKDOWN", "TXT").contains(extension)) {
            return "DOCUMENT";
        }
        throw new BusinessException("UNSUPPORTED_RESOURCE_TYPE", "Unsupported resource type: " + extension);
    }

    private String extractText(String extension, Path filePath) throws IOException {
        return switch (extension) {
            case "TXT", "MD", "MARKDOWN" -> Files.readString(filePath, StandardCharsets.UTF_8);
            case "PDF" -> extractPdf(filePath);
            case "DOCX" -> extractDocx(filePath);
            case "PPTX" -> extractPptx(filePath);
            case "PPT" -> extractPpt(filePath);
            default -> "";
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
                for (List<HSLFTextParagraph> group : slide.getTextParagraphs()) {
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

    private String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toUpperCase(Locale.ROOT);
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
