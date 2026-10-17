package com.tandem.tandem_backend.controller;

import com.tandem.tandem_backend.dto.ResolveConflictRequest;
import com.tandem.tandem_backend.entity.Conflict;
import com.tandem.tandem_backend.service.ConflictService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conflicts")
public class ConflictController {

    private final ConflictService conflictService;

    public ConflictController(ConflictService conflictService) {
        this.conflictService = conflictService;
    }

    @GetMapping
    public List<Conflict> getAllConflicts() {
        return conflictService.getAllConflicts();
    }

    @GetMapping("/open")
    public List<Conflict> getOpenConflicts() {
        return conflictService.getOpenConflicts();
    }

    @GetMapping("/record/{recordId}")
    public List<Conflict> getConflictsByRecord(
            @PathVariable Long recordId
    ) {
        return conflictService.getConflictsForRecord(recordId);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveConflict(
            @PathVariable Long id,
            @RequestBody ResolveConflictRequest request
    ) {
        try {
            Conflict resolvedConflict = conflictService.resolveConflict(
                    id,
                    request.getResolution()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Conflict resolved successfully",
                    "conflict", resolvedConflict
            ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));

        } catch (IllegalStateException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}