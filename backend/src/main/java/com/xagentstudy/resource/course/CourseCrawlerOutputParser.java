package com.xagentstudy.resource.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CourseCrawlerOutputParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<CourseCrawlerCandidate> parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                throw new BusinessException("CRAWLER_OUTPUT_INVALID", "课程爬虫返回格式不正确");
            }
            List<CourseCrawlerCandidate> candidates = new ArrayList<>();
            for (JsonNode node : root) {
                String title = text(node, "title");
                String url = text(node, "url");
                if (title.isBlank() || url.isBlank()) {
                    continue;
                }
                candidates.add(new CourseCrawlerCandidate(
                        title,
                        text(node, "description"),
                        url,
                        defaultText(node, "source", "公开课程"),
                        text(node, "coverUrl"),
                        defaultText(node, "subjectName", title),
                        defaultText(node, "subjectScope", "公开课程"),
                        tags(node.path("tags")),
                        text(node, "difficulty")
                ));
            }
            return candidates;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("CRAWLER_OUTPUT_INVALID", "课程爬虫返回格式不正确");
        }
    }

    private List<String> tags(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (!value.isBlank() && values.size() < 8) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String defaultText(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return value.isBlank() ? fallback : value;
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }
}
