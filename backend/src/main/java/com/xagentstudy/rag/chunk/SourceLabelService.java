package com.xagentstudy.rag.chunk;

import com.xagentstudy.knowledge.KnowledgeDocument;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SourceLabelService {
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public SourceLabelService(KnowledgeChunkRepository chunkRepository, KnowledgeDocumentRepository documentRepository) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
    }

    public String label(String sourceScope) {
        if (sourceScope == null || sourceScope.isBlank()) {
            return "未标记来源";
        }
        if ("COLD_START".equals(sourceScope)) {
            return "无知识库通用题";
        }
        if (sourceScope.startsWith("DOCUMENT:")) {
            Long documentId = parseIds(sourceScope.substring("DOCUMENT:".length())).stream().findFirst().orElse(null);
            if (documentId == null) {
                return sourceScope;
            }
            return documentRepository.findById(documentId)
                    .map(document -> "知识点文档：" + document.getName())
                    .orElse("知识点文档 #" + documentId);
        }
        List<Long> chunkIds = parseChunkIds(sourceScope);
        if (chunkIds.isEmpty()) {
            return sourceScope;
        }
        List<KnowledgeChunk> chunks = chunkRepository.findAllById(chunkIds);
        if (chunks.isEmpty()) {
            return sourceScope.startsWith("CHUNK:") ? "资料切片 #" + chunkIds.get(0) : "多资料切片 " + chunkIds;
        }
        Map<Long, KnowledgeDocument> documents = documentRepository.findAllById(
                        chunks.stream().map(KnowledgeChunk::getDocumentId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, Function.identity()));
        Set<String> labels = new LinkedHashSet<>();
        for (KnowledgeChunk chunk : chunks) {
            KnowledgeDocument document = documents.get(chunk.getDocumentId());
            String documentName = document == null ? "Document #" + chunk.getDocumentId() : document.getName();
            labels.add(documentName + " · " + chunk.getSourceLocation());
            if (labels.size() >= 3) {
                break;
            }
        }
        String suffix = chunks.size() > labels.size() ? " 等 " + chunks.size() + " 个切片" : "";
        return String.join("；", labels) + suffix;
    }

    private List<Long> parseChunkIds(String sourceScope) {
        if (sourceScope.startsWith("CHUNK:")) {
            return parseIds(sourceScope.substring("CHUNK:".length()));
        }
        if (sourceScope.startsWith("RAG:")) {
            return parseIds(sourceScope.substring("RAG:".length()));
        }
        return List.of();
    }

    private List<Long> parseIds(String value) {
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .flatMap(item -> {
                    try {
                        return java.util.stream.Stream.of(Long.valueOf(item));
                    } catch (NumberFormatException ignored) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }
}
