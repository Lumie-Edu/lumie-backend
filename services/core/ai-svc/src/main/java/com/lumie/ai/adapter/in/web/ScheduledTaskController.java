package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.service.ScheduledTaskCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheduled-tasks")
@RequiredArgsConstructor
public class ScheduledTaskController {

    private final ScheduledTaskCommandService commandService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listTasks() {
        return ResponseEntity.ok(commandService.listTasks());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(commandService.updateTask(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        commandService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
