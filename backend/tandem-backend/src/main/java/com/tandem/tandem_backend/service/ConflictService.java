package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Conflict;
import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.entity.Record;
import com.tandem.tandem_backend.repository.ConflictRepository;
import com.tandem.tandem_backend.repository.RecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConflictService {

    private final ConflictRepository conflictRepository;
    private final RecordRepository recordRepository;
    private final JsonMapper jsonMapper;

    public ConflictService(
            ConflictRepository conflictRepository,
            RecordRepository recordRepository,
            JsonMapper jsonMapper
    ) {
        this.conflictRepository = conflictRepository;
        this.recordRepository = recordRepository;
        this.jsonMapper = jsonMapper;
    }

    // Compatibility method for existing four-argument callers.
    @Transactional
    public Conflict createConflict(
            Long recordId,
            Operation existingOperation,
            String incomingOperationId,
            String incomingPayload
    ) {
        return createConflict(
                recordId,
                existingOperation,
                incomingOperationId,
                incomingPayload,
                "unknown"
        );
    }

    @Transactional
    public Conflict createConflict(
            Long recordId,
            Operation existingOperation,
            String incomingOperationId,
            String incomingPayload,
            String fieldName
    ) {
        Optional<Conflict> existingConflict =
                conflictRepository
                        .findFirstByRecordIdAndFieldNameAndStatus(
                                recordId,
                                fieldName,
                                "OPEN"
                        );

        if (existingConflict.isPresent()) {
            return existingConflict.get();
        }

        Conflict conflict = new Conflict();

        conflict.setRecordId(recordId);
        conflict.setIncomingOperationId(incomingOperationId);
        conflict.setExistingOperationId(
                existingOperation.getOperationId()
        );
        conflict.setFieldName(fieldName);
        conflict.setIncomingPayload(incomingPayload);
        conflict.setExistingPayload(existingOperation.getPayload());
        conflict.setStatus("OPEN");
        conflict.setCreatedAt(LocalDateTime.now());

        return conflictRepository.save(conflict);
    }

    public List<Conflict> getAllConflicts() {
        return conflictRepository.findAll();
    }

    public List<Conflict> getConflictsForRecord(Long recordId) {
        return conflictRepository.findByRecordIdOrderByCreatedAtAsc(
                recordId
        );
    }

    public List<Conflict> getOpenConflicts() {
        return conflictRepository.findByStatusOrderByCreatedAtAsc(
                "OPEN"
        );
    }

    @Transactional
    public Conflict resolveConflict(Long conflictId, String decision) {
        Conflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conflict not found: " + conflictId
                ));

        if (!"OPEN".equals(conflict.getStatus())) {
            throw new IllegalStateException(
                    "Conflict has already been resolved"
            );
        }

        if (!"KEEP_EXISTING".equals(decision)
                && !"KEEP_INCOMING".equals(decision)) {
            throw new IllegalArgumentException(
                    "Decision must be KEEP_EXISTING or KEEP_INCOMING"
            );
        }

        Record record = recordRepository.findById(
                conflict.getRecordId()
        ).orElseThrow(() -> new IllegalArgumentException(
                "Record not found: " + conflict.getRecordId()
        ));

        Map<String, Object> existingPayload =
                parsePayload(conflict.getExistingPayload());

        Map<String, Object> incomingPayload =
                parsePayload(conflict.getIncomingPayload());

        Map<String, Object> mergedPayload = new LinkedHashMap<>();
        mergedPayload.putAll(existingPayload);
        mergedPayload.putAll(incomingPayload);

        Map<String, Object> selectedPayload =
                "KEEP_EXISTING".equals(decision)
                        ? existingPayload
                        : incomingPayload;

        String fieldName = conflict.getFieldName();

        if (fieldName != null
                && selectedPayload.containsKey(fieldName)) {
            mergedPayload.put(
                    fieldName,
                    selectedPayload.get(fieldName)
            );
        }

        applyPayload(record, mergedPayload);
        recordRepository.save(record);

        conflict.setStatus("RESOLVED");

        return conflictRepository.save(conflict);
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }

        try {
            return jsonMapper.readValue(payload, Map.class);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to parse conflict payload",
                    exception
            );
        }
    }

    private void applyPayload(
            Record record,
            Map<String, Object> payload
    ) {
        if (payload.containsKey("name")) {
            record.setName(asString(payload.get("name")));
        }

        if (payload.containsKey("phone")) {
            record.setPhone(asString(payload.get("phone")));
        }

        if (payload.containsKey("village")) {
            record.setVillage(asString(payload.get("village")));
        }

        if (payload.containsKey("address")) {
            record.setAddress(asString(payload.get("address")));
        }

        if (payload.containsKey("status")) {
            record.setStatus(asString(payload.get("status")));
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}