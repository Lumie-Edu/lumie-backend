package com.lumie.spreadsheet.infrastructure.websocket;

import com.lumie.spreadsheet.application.service.CellLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final CellLockService cellLockService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connected: session={}", sessionId);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        if (destination != null && destination.startsWith("/topic/spreadsheet/")) {
            String spreadsheetId = destination.replace("/topic/spreadsheet/", "");
            Map<String, Object> attrs = headerAccessor.getSessionAttributes();
            String userId = attrs != null ? (String) attrs.get("userId") : null;
            String userName = attrs != null ? (String) attrs.get("userName") : null;

            log.info("User subscribed to spreadsheet: spreadsheetId={}, userId={}, userName={}, session={}",
                    spreadsheetId, userId, userName, sessionId);

            broadcastUserJoined(spreadsheetId, userId, userName, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();

        if (attrs != null) {
            String userId = (String) attrs.get("userId");
            String userName = (String) attrs.get("userName");
            String spreadsheetId = (String) attrs.get("currentSpreadsheetId");

            log.info("WebSocket disconnected: session={}, userId={}", sessionId, userId);

            if (userId != null) {
                cellLockService.releaseAllLocksForUser(userId);
            }

            if (spreadsheetId != null && userId != null) {
                broadcastUserLeft(spreadsheetId, userId, userName, sessionId);
            }
        }
    }

    private void broadcastUserJoined(String spreadsheetId, String userId, String userName, String sessionId) {
        Map<String, Object> message = Map.of(
                "type", "USER_JOINED",
                "userId", userId != null ? userId : "",
                "userName", userName != null ? userName : "Anonymous",
                "sessionId", sessionId
        );
        messagingTemplate.convertAndSend("/topic/spreadsheet/" + spreadsheetId, message);
    }

    private void broadcastUserLeft(String spreadsheetId, String userId, String userName, String sessionId) {
        Map<String, Object> message = Map.of(
                "type", "USER_LEFT",
                "userId", userId != null ? userId : "",
                "userName", userName != null ? userName : "Anonymous",
                "sessionId", sessionId
        );
        messagingTemplate.convertAndSend("/topic/spreadsheet/" + spreadsheetId, message);
    }
}
