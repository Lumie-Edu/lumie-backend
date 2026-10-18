package com.lumie.ai.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.ai.application.dto.response.ConversationDetailResponse;
import com.lumie.ai.application.dto.response.ConversationSummaryResponse;
import com.lumie.ai.application.service.ConversationQueryService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationQueryService queryService;

    @GetMapping
    @AllowedSortFields({"createdAt", "updatedAt"})
    public ResponseEntity<PageResponse<ConversationSummaryResponse>> listConversations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        requireStaffUser();
        return ResponseEntity.ok(PageResponse.from(queryService.listConversations(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversation(@PathVariable Long id) {
        requireStaffUser();
        return ResponseEntity.ok(queryService.getConversation(id));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        requireStaffUser();
        queryService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    private void requireStaffUser() {
        Role role = UserContextHolder.getUserRole();
        if (role == null || role == Role.STUDENT) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
