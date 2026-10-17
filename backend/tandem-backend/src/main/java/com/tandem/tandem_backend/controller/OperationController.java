package com.tandem.tandem_backend.controller;

import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.service.OperationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping
    public ResponseEntity<Operation> createOperation(
            @RequestBody Operation operation) {

        return ResponseEntity.ok(
                operationService.createOperation(operation)
        );
    }

    @GetMapping
    public ResponseEntity<List<Operation>> getAllOperations() {
        return ResponseEntity.ok(
                operationService.getAllOperations()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> getOperationById(
            @PathVariable Long id) {

        return operationService.getOperationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/operation/{operationId}")
    public ResponseEntity<Operation> getByOperationId(
            @PathVariable String operationId) {

        return operationService.getOperationByOperationId(operationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}