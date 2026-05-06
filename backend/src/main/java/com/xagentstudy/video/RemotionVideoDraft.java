package com.xagentstudy.video;

import java.util.List;

public record RemotionVideoDraft(
        String title,
        String sourceCode,
        List<RemotionScene> scenes
) {
}
