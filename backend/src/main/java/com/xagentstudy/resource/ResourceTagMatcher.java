package com.xagentstudy.resource;

import java.util.List;
import java.util.Locale;

final class ResourceTagMatcher {
    private ResourceTagMatcher() {
    }

    static boolean matches(String directionName, String directionCategory, LearningResource resource) {
        String direction = normalize(directionName);
        String category = normalize(directionCategory);
        List<String> candidates = List.of(
                normalize(resource.getSubjectName()),
                normalize(resource.getSubjectScope()),
                normalize(resource.getTags())
        );
        return candidates.stream().anyMatch(candidate ->
                containsEither(candidate, direction, category)
                        || containsEither(direction, candidate, "")
                        || containsEither(category, candidate, "")
        );
    }

    private static boolean containsEither(String source, String first, String second) {
        return !source.isBlank()
                && ((!first.isBlank() && source.contains(first))
                || (!second.isBlank() && source.contains(second)));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
