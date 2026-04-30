package com.xagentstudy.rag.chunk;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocalEmbeddingService implements EmbeddingProvider {
    private static final int DIMENSIONS = 64;

    @Override
    public List<Double> embed(String text) {
        double[] vector = new double[DIMENSIONS];
        String normalized = text == null ? "" : text.toLowerCase();
        List<String> tokens = tokens(normalized);
        for (String token : tokens) {
            int index = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[index] += 1.0d;
        }
        double norm = 0.0d;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(DIMENSIONS);
        for (double value : vector) {
            result.add(norm == 0.0d ? 0.0d : value / norm);
        }
        return result;
    }

    private List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String part : text.split("[\\s,，。！？?;；:：、]+")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                tokens.add(trimmed);
            }
        }
        for (int i = 0; i < text.length(); i += 2) {
            int end = Math.min(i + 2, text.length());
            String gram = text.substring(i, end).trim();
            if (gram.length() >= 2) {
                tokens.add(gram);
            }
        }
        return tokens;
    }
}
