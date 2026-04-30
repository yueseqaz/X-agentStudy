package com.xagentstudy.knowledge;

import com.xagentstudy.common.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/documents")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService service;

    public KnowledgeDocumentController(KnowledgeDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list(@PathVariable Long planId) {
        return ApiResponse.ok(service.list(planId));
    }

    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponse> detail(@PathVariable Long planId, @PathVariable Long documentId) {
        return ApiResponse.ok(service.detail(planId, documentId));
    }

    @GetMapping("/{documentId}/content")
    public ApiResponse<DocumentContentResponse> content(@PathVariable Long planId, @PathVariable Long documentId) {
        return ApiResponse.ok(service.content(planId, documentId));
    }

    @PostMapping
    public ApiResponse<DocumentUploadResponse> upload(@PathVariable Long planId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(planId, file));
    }

    @PostMapping("/generate")
    public ApiResponse<GeneratedOnlineDocumentResponse> generateOnlineDocument(
            @PathVariable Long planId,
            @RequestBody GenerateOnlineDocumentRequest request
    ) {
        return ApiResponse.ok(service.generateOnlineDocument(planId, request));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(@PathVariable Long planId, @PathVariable Long documentId) {
        service.delete(planId, documentId);
        return ApiResponse.ok(null);
    }
}
