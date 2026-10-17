package com.tandem.tandem_backend.controller;

import com.tandem.tandem_backend.dto.UpdateRecordRequest;
import com.tandem.tandem_backend.entity.Record;
import com.tandem.tandem_backend.service.RecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    // Create a record
    @PostMapping
    public ResponseEntity<Record> createRecord(
            @RequestBody Record record) {

        Record createdRecord =
                recordService.createRecord(record);

        return ResponseEntity.ok(createdRecord);
    }

    // Get all records
    @GetMapping
    public ResponseEntity<List<Record>> getAllRecords() {

        return ResponseEntity.ok(
                recordService.getAllRecords()
        );
    }

    // Get record by ID
    @GetMapping("/{id}")
    public ResponseEntity<Record> getRecordById(
            @PathVariable Long id) {

        return recordService.getRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update a record
    @PutMapping("/{id}")
    public ResponseEntity<Record> updateRecord(
            @PathVariable Long id,
            @RequestBody UpdateRecordRequest request) {

        return recordService.updateRecord(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete a record
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long id) {

        recordService.deleteRecord(id);

        return ResponseEntity.noContent().build();
    }
}