package com.lumie.ai.application.port.out;

import java.util.List;
import java.util.Map;

public interface AiServicePort {

    Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools);
}
