package com.lumie.spreadsheet.infrastructure.websocket;

import com.lumie.common.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TenantChannelInterceptor implements ChannelInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Slug";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_NAME_HEADER = "X-User-Name";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String tenantSlug = accessor.getFirstNativeHeader(TENANT_HEADER);
            String userId = accessor.getFirstNativeHeader(USER_ID_HEADER);
            String userName = accessor.getFirstNativeHeader(USER_NAME_HEADER);

            if (tenantSlug != null) {
                accessor.getSessionAttributes().put("tenantSlug", tenantSlug);
                log.debug("WebSocket connection established for tenant: {}", tenantSlug);
            }

            if (userId != null) {
                accessor.getSessionAttributes().put("userId", userId);
                accessor.setUser(() -> userId);
            }

            if (userName != null) {
                accessor.getSessionAttributes().put("userName", userName);
            }
        }

        if (accessor != null && (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                                  StompCommand.SEND.equals(accessor.getCommand()))) {
            Object tenantSlug = accessor.getSessionAttributes().get("tenantSlug");
            if (tenantSlug != null) {
                TenantContextHolder.setTenant(tenantSlug.toString());
            }
        }

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        TenantContextHolder.clear();
    }
}
