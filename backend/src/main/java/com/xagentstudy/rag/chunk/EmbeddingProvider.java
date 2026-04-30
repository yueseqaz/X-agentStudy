package com.xagentstudy.rag.chunk;

import java.util.List;

public interface EmbeddingProvider {
    List<Double> embed(String text);

    default double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        double score = 0.0d;
        for (int i = 0; i < size; i++) {
            score += left.get(i) * right.get(i);
        }
        return score;
    }
}
