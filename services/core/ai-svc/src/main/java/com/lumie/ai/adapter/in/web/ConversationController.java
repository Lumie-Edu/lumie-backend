package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.service.ConversationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> listConversations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listConversations(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getConversation(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        queryService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
