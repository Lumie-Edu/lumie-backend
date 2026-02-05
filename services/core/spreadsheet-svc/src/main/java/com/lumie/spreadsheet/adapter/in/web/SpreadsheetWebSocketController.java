package com.lumie.spreadsheet.adapter.in.web;

import com.lumie.spreadsheet.application.dto.request.UpdateCellRequest;
import com.lumie.spreadsheet.application.dto.response.CellResponse;
import com.lumie.spreadsheet.application.dto.websocket.CellLockMessage;
import com.lumie.spreadsheet.application.dto.websocket.CellUpdateMessage;
import com.lumie.spreadsheet.application.dto.websocket.WebSocketResponse;
import com.lumie.spreadsheet.application.service.CellLockService;
import com.lumie.spreadsheet.application.service.SpreadsheetCommandService;
import com.lumie.spreadsheet.domain.exception.SpreadsheetException;
import com.lumie.spreadsheet.domain.vo.CellData;
import com.lumie.spreadsheet.domain.vo.CellLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SpreadsheetWebSocketController {

    private final CellLockService cellLockService;
    private final SpreadsheetCommandService spreadsheetCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/spreadsheet/{spreadsheetId}/lock")
    @SendToUser("/queue/reply")
    public WebSocketResponse requestCellLock(
            @DestinationVariable String spreadsheetId,
            @Payload CellLockMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        String userId = attrs != null ? (String) attrs.get("userId") : message.userId();
        String userName = attrs != null ? (String) attrs.get("userName") : message.userName();

        try {
            CellLock lock = cellLockService.acquireLock(spreadsheetId, message.cellAddress(), userId, userName);

            messagingTemplate.convertAndSend(
                    "/topic/spreadsheet/" + spreadsheetId,
                    WebSocketResponse.cellLockAcquired(lock)
            );

            return WebSocketResponse.cellLockAcquired(lock);
        } catch (SpreadsheetException e) {
            log.warn("Cell lock denied: spreadsheet={}, cell={}, user={}, reason={}",
                    spreadsheetId, message.cellAddress(), userId, e.getMessage());

            return WebSocketResponse.cellLockDenied(message.cellAddress(), e.getMessage());
        }
    }

    @MessageMapping("/spreadsheet/{spreadsheetId}/update")
    public void updateCell(
            @DestinationVariable String spreadsheetId,
            @Payload CellUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        String userId = attrs != null ? (String) attrs.get("userId") : message.userId();
        String userName = attrs != null ? (String) attrs.get("userName") : message.userName();

        try {
            UpdateCellRequest request = new UpdateCellRequest(
                    message.cellAddress(),
                    message.value(),
                    message.formula(),
                    message.style()
            );

            CellResponse response = spreadsheetCommandService.updateCell(Long.parseLong(spreadsheetId), request);

            CellUpdateMessage broadcastMessage = new CellUpdateMessage(
                    response.address(),
                    response.value(),
                    response.formula(),
                    response.displayValue(),
                    response.style(),
                    userId,
                    userName
            );

            messagingTemplate.convertAndSend(
                    "/topic/spreadsheet/" + spreadsheetId,
                    WebSocketResponse.cellUpdated(broadcastMessage)
            );

            log.debug("Cell updated via WebSocket: spreadsheet={}, cell={}, user={}",
                    spreadsheetId, message.cellAddress(), userId);

        } catch (Exception e) {
            log.error("Failed to update cell via WebSocket: spreadsheet={}, cell={}, user={}, error={}",
                    spreadsheetId, message.cellAddress(), userId, e.getMessage());

            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/errors",
                    WebSocketResponse.error("Failed to update cell: " + e.getMessage())
            );
        }
    }

    @MessageMapping("/spreadsheet/{spreadsheetId}/unlock")
    public void releaseCellLock(
            @DestinationVariable String spreadsheetId,
            @Payload CellLockMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        String userId = attrs != null ? (String) attrs.get("userId") : message.userId();

        cellLockService.releaseLock(spreadsheetId, message.cellAddress(), userId);

        messagingTemplate.convertAndSend(
                "/topic/spreadsheet/" + spreadsheetId,
                WebSocketResponse.cellUnlocked(message.cellAddress(), userId)
        );

        log.debug("Cell unlocked via WebSocket: spreadsheet={}, cell={}, user={}",
                spreadsheetId, message.cellAddress(), userId);
    }

    @MessageMapping("/spreadsheet/{spreadsheetId}/refresh-lock")
    public void refreshCellLock(
            @DestinationVariable String spreadsheetId,
            @Payload CellLockMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        String userId = attrs != null ? (String) attrs.get("userId") : message.userId();

        cellLockService.refreshLock(spreadsheetId, message.cellAddress(), userId);

        log.debug("Cell lock refreshed via WebSocket: spreadsheet={}, cell={}, user={}",
                spreadsheetId, message.cellAddress(), userId);
    }
}
