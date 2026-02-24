package com.lumie.ai.application.port.out;

import java.util.Map;

public interface ContentServicePort {

    String createAnnouncement(Map<String, Object> arguments);
}
