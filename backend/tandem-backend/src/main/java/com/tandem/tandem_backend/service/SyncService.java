package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.dto.SyncOperationRequest;
import com.tandem.tandem_backend.dto.SyncOperationResponse;
import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.entity.PendingOperation;
import com.tandem.tandem_backend.entity.Record;
import com.tandem.tandem_backend.repository.OperationRepository;
import com.tandem.tandem_backend.repository.PendingOperationRepository;
import com.tandem.tandem_backend.repository.RecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SyncService {

    private final RecordRepository recordRepository;
    private final OperationRepository operationRepository;
    private final PendingOperationRepository pendingOperationRepository;
    private final CausalConflictService causalConflictService;
    private final ConflictResolutionService conflictResolutionService;
    private final ConflictService conflictService;
    private final JsonMapper jsonMapper;

    public SyncService(
            RecordRepository recordRepository,
            OperationRepository operationRepository,
            PendingOperationRepository pendingOperationRepository,
            CausalConflictService causalConflictService,
            ConflictResolutionService conflictResolutionService,
            ConflictService conflictService,
            JsonMapper jsonMapper
    ) {
        this.recordRepository = recordRepository;
        this.operationRepository = operationRepository;
        this.pendingOperationRepository =
                pendingOperationRepository;
        this.causalConflictService =
                causalConflictService;
        this.conflictResolutionService =
                conflictResolutionService;
        this.conflictService =
                conflictService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public SyncOperationResponse processOperation(
            SyncOperationRequest request
    ) {
        Optional<Operation> existingOperation =
                operationRepository.findByOperationId(
                        request.getOperationId()
                );

        if (existingOperation.isPresent()) {
            Operation operation =
                    existingOperation.get();

            return new SyncOperationResponse(
                    request.getOperationId(),
                    true,
                    operation.getRecordId(),
                    "Operation already processed",
                    SyncOperationResponse.Status.DUPLICATE
            );
        }

        Optional<PendingOperation> existingPending =
                pendingOperationRepository.findByOperationId(
                        request.getOperationId()
                );

        if (existingPending.isPresent()) {
            return new SyncOperationResponse(
                    request.getOperationId(),
                    true,
                    request.getRecordId(),
                    "Operation already buffered",
                    SyncOperationResponse.Status.BUFFERED
            );
        }

        if (hasSequenceGap(request)) {
            bufferOperation(request);

            Long expectedSequence =
                    getExpectedSequence(
                            request.getDeviceId()
                    );

            return new SyncOperationResponse(
                    request.getOperationId(),
                    true,
                    request.getRecordId(),
                    "Operation buffered. Waiting for sequence "
                            + expectedSequence,
                    SyncOperationResponse.Status.BUFFERED
            );
        }

        if (isStaleSequence(request)) {
            return new SyncOperationResponse(
                    request.getOperationId(),
                    false,
                    request.getRecordId(),
                    "Operation sequence is older than the latest "
                            + "processed sequence",
                    SyncOperationResponse.Status.FAILED
            );
        }

        ApplyResult result =
                applyOperation(request);

        drainPendingOperations(
                request.getDeviceId()
        );

        if (result.conflictDetected()) {
            return new SyncOperationResponse(
                    request.getOperationId(),
                    true,
                    result.recordId(),
                    "Conflict detected. Operation was stored "
                            + "but the record was not modified",
                    SyncOperationResponse.Status.APPLIED
            );
        }

        return new SyncOperationResponse(
                request.getOperationId(),
                true,
                result.recordId(),
                "Operation synchronized successfully",
                SyncOperationResponse.Status.APPLIED
        );
    }

    private ApplyResult applyOperation(
            SyncOperationRequest request
    ) {
        boolean conflictDetected =
                detectConflicts(request);

        if (conflictDetected) {

            Long recordId =
                    request.getRecordId();

            saveOperation(
                    request,
                    recordId
            );

            return new ApplyResult(
                    recordId,
                    true
            );
        }

        Long serverRecordId;

        switch (request.getOperationType()) {

            case "CREATE":
                serverRecordId =
                        createRecord(request);
                break;

            case "UPDATE":
                serverRecordId =
                        updateRecord(request);
                break;

            case "DELETE":
                serverRecordId =
                        deleteRecord(request);
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported operation type: "
                                + request.getOperationType()
                );
        }

        saveOperation(
                request,
                serverRecordId
        );

        return new ApplyResult(
                serverRecordId,
                false
        );
    }

    private boolean hasSequenceGap(
            SyncOperationRequest request
    ) {
        Long sequenceNumber =
                request.getSequenceNumber();

        if (sequenceNumber == null) {
            return false;
        }

        Optional<Operation> latestOperation =
                operationRepository
                        .findTopByDeviceIdOrderBySequenceNumberDesc(
                                request.getDeviceId()
                        );

        if (latestOperation.isEmpty()) {
            return false;
        }

        Long latestSequence =
                latestOperation.get().getSequenceNumber();

        if (latestSequence == null) {
            return false;
        }

        long expectedSequence =
                latestSequence + 1;

        return sequenceNumber > expectedSequence;
    }

    private boolean isStaleSequence(
            SyncOperationRequest request
    ) {
        Long sequenceNumber =
                request.getSequenceNumber();

        if (sequenceNumber == null) {
            return false;
        }

        Optional<Operation> latestOperation =
                operationRepository
                        .findTopByDeviceIdOrderBySequenceNumberDesc(
                                request.getDeviceId()
                        );

        if (latestOperation.isEmpty()) {
            return false;
        }

        Long latestSequence =
                latestOperation.get().getSequenceNumber();

        if (latestSequence == null) {
            return false;
        }

        return sequenceNumber <= latestSequence;
    }

    private Long getExpectedSequence(
            String deviceId
    ) {
        Optional<Operation> latestOperation =
                operationRepository
                        .findTopByDeviceIdOrderBySequenceNumberDesc(
                                deviceId
                        );

        if (latestOperation.isEmpty()) {
            return 1L;
        }

        Long latestSequence =
                latestOperation.get().getSequenceNumber();

        if (latestSequence == null) {
            return 1L;
        }

        return latestSequence + 1;
    }

    private void bufferOperation(
            SyncOperationRequest request
    ) {
        PendingOperation pendingOperation =
                new PendingOperation();

        pendingOperation.setOperationId(
                request.getOperationId()
        );

        pendingOperation.setRecordId(
                request.getRecordId()
        );

        pendingOperation.setDeviceId(
                request.getDeviceId()
        );

        pendingOperation.setOperationType(
                request.getOperationType()
        );

        pendingOperation.setPayload(
                serializePayload(
                        request.getPayload()
                )
        );

        pendingOperation.setSequenceNumber(
                request.getSequenceNumber()
        );

        pendingOperation.setCausalContext(
                serializeCausalContext(
                        request.getCausalContext()
                )
        );

        if (request.getCreatedAt() != null) {
            pendingOperation.setCreatedAt(
                    request.getCreatedAt()
                            .toEpochMilli()
            );
        } else {
            pendingOperation.setCreatedAt(
                    System.currentTimeMillis()
            );
        }

        pendingOperationRepository.save(
                pendingOperation
        );

        System.out.println(
                "OPERATION BUFFERED: operationId="
                        + request.getOperationId()
                        + ", device="
                        + request.getDeviceId()
                        + ", sequence="
                        + request.getSequenceNumber()
        );
    }

    private void drainPendingOperations(
            String deviceId
    ) {
        while (true) {

            Long expectedSequence =
                    getExpectedSequence(deviceId);

            Optional<PendingOperation> pendingOperation =
                    pendingOperationRepository
                            .findByDeviceIdAndSequenceNumber(
                                    deviceId,
                                    expectedSequence
                            );

            if (pendingOperation.isEmpty()) {
                return;
            }

            PendingOperation pending =
                    pendingOperation.get();

            SyncOperationRequest request =
                    convertToRequest(pending);

            pendingOperationRepository.delete(
                    pending
            );

            applyOperation(request);

            System.out.println(
                    "BUFFERED OPERATION APPLIED: "
                            + "operationId="
                            + request.getOperationId()
                            + ", device="
                            + deviceId
                            + ", sequence="
                            + request.getSequenceNumber()
            );
        }
    }

    private SyncOperationRequest convertToRequest(
            PendingOperation pendingOperation
    ) {
        SyncOperationRequest request =
                new SyncOperationRequest();

        request.setOperationId(
                pendingOperation.getOperationId()
        );

        request.setRecordId(
                pendingOperation.getRecordId()
        );

        request.setDeviceId(
                pendingOperation.getDeviceId()
        );

        request.setOperationType(
                pendingOperation.getOperationType()
        );

        request.setPayload(
                deserializePayload(
                        pendingOperation.getPayload()
                )
        );

        request.setSequenceNumber(
                pendingOperation.getSequenceNumber()
        );

        request.setCausalContext(
                deserializeCausalContext(
                        pendingOperation.getCausalContext()
                )
        );

        if (pendingOperation.getCreatedAt() != null) {
            request.setCreatedAt(
                    Instant.ofEpochMilli(
                            pendingOperation.getCreatedAt()
                    )
            );
        }

        return request;
    }

    private boolean detectConflicts(SyncOperationRequest request) {
        if (request.getRecordId() == null
                || request.getCausalContext() == null
                || request.getPayload() == null) {
            return false;
        }

        List<Operation> concurrentOperations =
                causalConflictService.findConcurrentOperations(
                        request.getRecordId(),
                        request.getCausalContext(),
                        request.getDeviceId(),
                        request.getSequenceNumber()
                );

        if (concurrentOperations.isEmpty()) {
            return false;
        }

        boolean conflictDetected = false;

        for (Operation existingOperation : concurrentOperations) {
            if (request.getOperationId().equals(
                    existingOperation.getOperationId())) {
                continue;
            }

            ConflictResolutionService.ResolutionResult result =
                    conflictResolutionService.resolve(
                            request.getPayload(),
                            existingOperation
                    );

            if (result.getType()
                    == ConflictResolutionService.ResolutionType.FIELD_CONFLICT) {

                conflictDetected = true;

                conflictService.createConflict(
                        request.getRecordId(),
                        existingOperation,
                        request.getOperationId(),
                        serializePayload(request.getPayload()),
                        result.getConflictingField()
                );

                System.out.println(
                        "FIELD CONFLICT DETECTED: incoming="
                                + request.getOperationId()
                                + ", existing="
                                + existingOperation.getOperationId()
                                + ", recordId="
                                + request.getRecordId()
                                + ", field="
                                + result.getConflictingField()
                );

            } else if (result.getType()
                    == ConflictResolutionService.ResolutionType.MERGE) {

                System.out.println(
                        "CONCURRENT OPERATIONS CAN MERGE: incoming="
                                + request.getOperationId()
                                + ", existing="
                                + existingOperation.getOperationId()
                                + ", recordId="
                                + request.getRecordId()
                );
            }
        }

        return conflictDetected;
    }

    private Long createRecord(
            SyncOperationRequest request
    ) {
        Map<String, Object> payload =
                request.getPayload();

        Record record =
                new Record();

        record.setName(
                (String) payload.get("name")
        );

        record.setPhone(
                (String) payload.get("phone")
        );

        record.setVillage(
                (String) payload.get("village")
        );

        record.setAddress(
                (String) payload.get("address")
        );

        record.setStatus(
                (String) payload.get("status")
        );

        return recordRepository
                .save(record)
                .getId();
    }

    private Long updateRecord(
            SyncOperationRequest request
    ) {
        Optional<Record> optionalRecord =
                recordRepository.findById(
                        request.getRecordId()
                );

        if (optionalRecord.isEmpty()) {
            throw new IllegalArgumentException(
                    "Record not found: "
                            + request.getRecordId()
            );
        }

        Record record =
                optionalRecord.get();

        Map<String, Object> payload =
                request.getPayload();

        if (payload.containsKey("name")) {
            record.setName(
                    (String) payload.get("name")
            );
        }

        if (payload.containsKey("phone")) {
            record.setPhone(
                    (String) payload.get("phone")
            );
        }

        if (payload.containsKey("village")) {
            record.setVillage(
                    (String) payload.get("village")
            );
        }

        if (payload.containsKey("address")) {
            record.setAddress(
                    (String) payload.get("address")
            );
        }

        if (payload.containsKey("status")) {
            record.setStatus(
                    (String) payload.get("status")
            );
        }

        return recordRepository
                .save(record)
                .getId();
    }

    private Long deleteRecord(
            SyncOperationRequest request
    ) {
        Long recordId =
                request.getRecordId();

        if (recordRepository.existsById(recordId)) {
            recordRepository.deleteById(recordId);
        }

        return recordId;
    }

    private void saveOperation(
            SyncOperationRequest request,
            Long serverRecordId
    ) {
        Operation operation =
                new Operation();

        operation.setOperationId(
                request.getOperationId()
        );

        operation.setRecordId(
                serverRecordId
        );

        operation.setDeviceId(
                request.getDeviceId()
        );

        operation.setOperationType(
                request.getOperationType()
        );

        operation.setFieldName(
                "RECORD"
        );

        operation.setOldValue(null);

        operation.setNewValue(null);

        operation.setSequenceNumber(
                request.getSequenceNumber()
        );

        operation.setPayload(
                serializePayload(
                        request.getPayload()
                )
        );

        operation.setCausalContext(
                serializeCausalContext(
                        request.getCausalContext()
                )
        );

        operationRepository.save(
                operation
        );
    }

    private String serializePayload(
            Map<String, Object> payload
    ) {
        if (payload == null) {
            return null;
        }

        try {
            return jsonMapper.writeValueAsString(
                    payload
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to serialize operation payload",
                    exception
            );
        }
    }

    private String serializeCausalContext(
            Map<String, Long> causalContext
    ) {
        if (causalContext == null) {
            return null;
        }

        try {
            return jsonMapper.writeValueAsString(
                    causalContext
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to serialize causal context",
                    exception
            );
        }
    }

    private Map<String, Object> deserializePayload(
            String payload
    ) {
        if (payload == null) {
            return null;
        }

        try {
            return jsonMapper.readValue(
                    payload,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to deserialize pending operation payload",
                    exception
            );
        }
    }

    private Map<String, Long> deserializeCausalContext(
            String causalContext
    ) {
        if (causalContext == null) {
            return null;
        }

        try {
            return jsonMapper.readValue(
                    causalContext,
                    new TypeReference<Map<String, Long>>() {
                    }
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to deserialize pending causal context",
                    exception
            );
        }
    }

    private record ApplyResult(
            Long recordId,
            boolean conflictDetected
    ) {
    }
}