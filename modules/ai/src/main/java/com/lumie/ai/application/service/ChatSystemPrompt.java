package com.lumie.ai.application.service;

/**
 * Holds the externalized AI chat system-prompt prefix
 * (loaded from {@code classpath:prompts/chat-system.txt}).
 * The per-tenant schema header is appended at call time.
 */
public record ChatSystemPrompt(String prefix) {}
