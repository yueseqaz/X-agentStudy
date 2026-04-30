package com.xagentstudy.agent.model;

import java.util.Optional;

public interface ModelGateway {
    boolean hasConfiguredModel();

    Optional<String> generateJson(String systemPrompt, String userPrompt);

    Optional<String> generateText(String systemPrompt, String userPrompt);
}
