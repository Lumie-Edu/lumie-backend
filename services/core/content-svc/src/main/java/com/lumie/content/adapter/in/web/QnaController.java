package com.lumie.content.adapter.in.web;

import com.lumie.content.application.dto.request.CreateCommentRequest;
import com.lumie.content.application.dto.request.CreateQnaRequest;
import com.lumie.content.application.dto.request.UpdateQnaRequest;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaCommentResponse;
import com.lumie.content.application.dto.response.QnaDetailResponse;
import com.lumie.content.application.service.QnaCommandService;
import com.lumie.content.application.service.QnaQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaCommandService commandService;
    private final QnaQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<QnaBoardResponse>> listQnas(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listQnas(pageable));
    }

    @GetMapping("/unanswered")
    public ResponseEntity<Page<QnaBoardResponse>> listUnansweredQnas(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listUnansweredQnas(pageable));
    }

    @GetMapping("/user/{qnaUserId}")
    public ResponseEntity<Page<QnaBoardResponse>> listQnasByUser(
            @PathVariable Long qnaUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listQnasByUser(qnaUserId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QnaDetailResponse> getQna(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getQna(id));
    }

    @PostMapping
    public ResponseEntity<QnaBoardResponse> createQna(@Valid @RequestBody CreateQnaRequest request) {
        QnaBoardResponse response = commandService.createQna(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<QnaBoardResponse> updateQna(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQnaRequest request) {
        return ResponseEntity.ok(commandService.updateQna(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQna(@PathVariable Long id) {
        commandService.deleteQna(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<QnaCommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        QnaCommentResponse response = commandService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{qnaId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long qnaId,
            @PathVariable Long commentId) {
        commandService.deleteComment(qnaId, commentId);
        return ResponseEntity.noContent().build();
    }
}
