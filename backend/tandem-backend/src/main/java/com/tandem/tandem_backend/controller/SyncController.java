package com.tandem.tandem_backend.controller;

import com.tandem.tandem_backend.dto.SyncOperationRequest;
import com.tandem.tandem_backend.dto.SyncOperationResponse;
import com.tandem.tandem_backend.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public ResponseEntity<SyncOperationResponse> syncOperation(
            @RequestBody SyncOperationRequest request) {

        SyncOperationResponse response =
                syncService.processOperation(request);

        return ResponseEntity.ok(response);
    }
}