package com.lumie.ai.application.port.out;

import java.util.List;
import java.util.Map;

public interface AiServicePort {

    // Key under which adapters place a {prompt_tokens, completion_tokens, total_tokens, model}
    // map on the returned message, for observability. Callers that don't care can ignore it.
    String USAGE_KEY = "_usage";

    Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools);
}
