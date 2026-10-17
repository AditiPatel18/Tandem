package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.dto.SyncOperationRequest;
import com.tandem.tandem_backend.dto.SyncOperationResponse;
import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.entity.PendingOperation;
import com.tandem.tandem_backend.entity.Record;
import com.tandem.tandem_backend.repository.OperationRepository;
import com.tandem.tandem_backend.repository.PendingOperationRepository;
import com.tandem.tandem_backend.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private CausalConflictService causalConflictService;

    @Mock
    private ConflictResolutionService conflictResolutionService;

    @Mock
    private ConflictService conflictService;

    @Mock
    private PendingOperationRepository pendingOperationRepository;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(
                recordRepository,
                operationRepository,
                pendingOperationRepository,
                causalConflictService,
                conflictResolutionService,
                conflictService,
                JsonMapper.builder().build()
        );
    }

    @Test
    void shouldPersistPayloadAndCausalContextWhenOperationIsSynced() {
        SyncOperationRequest request = new SyncOperationRequest();
        request.setOperationId("operation-1");
        request.setRecordId(25L);
        request.setDeviceId("device-a");
        request.setOperationType("UPDATE");
        request.setPayload(Map.of("address", "New Address"));
        request.setCausalContext(Map.of("device-a", 1L));
        request.setSequenceNumber(1L);

        Record record = new Record();
        record.setId(25L);
        record.setAddress("Old Address");

        when(operationRepository.findByOperationId("operation-1"))
                .thenReturn(Optional.empty());
        when(pendingOperationRepository.findByOperationId("operation-1"))
                .thenReturn(Optional.empty());
        when(operationRepository.findTopByDeviceIdOrderBySequenceNumberDesc("device-a"))
                .thenReturn(Optional.empty());
        when(causalConflictService.findConcurrentOperations(
                25L, Map.of("device-a", 1L), "device-a", 1L))
                .thenReturn(List.of());
        when(recordRepository.findById(25L)).thenReturn(Optional.of(record));
        when(recordRepository.save(any(Record.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(operationRepository.save(any(Operation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SyncOperationResponse response = syncService.processOperation(request);

        assertTrue(response.isSuccess());
        assertEquals("New Address", record.getAddress());

        ArgumentCaptor<Operation> operationCaptor =
                ArgumentCaptor.forClass(Operation.class);

        verify(operationRepository).save(operationCaptor.capture());

        Operation savedOperation = operationCaptor.getValue();

        assertEquals("operation-1", savedOperation.getOperationId());
        assertEquals(25L, savedOperation.getRecordId());
        assertEquals("device-a", savedOperation.getDeviceId());
        assertEquals("UPDATE", savedOperation.getOperationType());
        assertEquals(1L, savedOperation.getSequenceNumber());
        assertEquals("{\"address\":\"New Address\"}", savedOperation.getPayload());
        assertEquals("{\"device-a\":1}", savedOperation.getCausalContext());
    }

    @Test
    void shouldDetectFieldConflictThroughSyncService() {
        SyncOperationRequest request = new SyncOperationRequest();
        request.setOperationId("incoming-operation");
        request.setRecordId(25L);
        request.setDeviceId("device-b");
        request.setOperationType("UPDATE");
        request.setPayload(Map.of("address", "New Address"));
        request.setCausalContext(Map.of(
                "device-a", 1L,
                "device-b", 1L
        ));
        request.setSequenceNumber(1L);

        Operation existingOperation = new Operation();
        existingOperation.setOperationId("existing-operation");
        existingOperation.setRecordId(25L);
        existingOperation.setPayload("{\"address\":\"Old Address\"}");

        when(operationRepository.findByOperationId("incoming-operation"))
                .thenReturn(Optional.empty());
        when(pendingOperationRepository.findByOperationId("incoming-operation"))
                .thenReturn(Optional.empty());
        when(operationRepository.findTopByDeviceIdOrderBySequenceNumberDesc("device-b"))
                .thenReturn(Optional.empty());
        when(causalConflictService.findConcurrentOperations(
                25L,
                Map.of("device-a", 1L, "device-b", 1L),
                "device-b",
                1L
        )).thenReturn(List.of(existingOperation));

        when(conflictResolutionService.resolve(
                Map.of("address", "New Address"),
                existingOperation
        )).thenReturn(new ConflictResolutionService.ResolutionResult(
                ConflictResolutionService.ResolutionType.FIELD_CONFLICT,
                Map.of("address", "New Address"),
                "address",
                "Same field conflict"
        ));

        when(operationRepository.save(any(Operation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SyncOperationResponse response = syncService.processOperation(request);

        assertTrue(response.isSuccess());

        verify(causalConflictService).findConcurrentOperations(
                25L,
                Map.of("device-a", 1L, "device-b", 1L),
                "device-b",
                1L
        );

        verify(conflictResolutionService).resolve(
                Map.of("address", "New Address"),
                existingOperation
        );

        verify(conflictService).createConflict(
                25L,
                existingOperation,
                "incoming-operation",
                "{\"address\":\"New Address\"}",
                "address"
        );

        verify(recordRepository, never()).save(any(Record.class));
    }

    @Test
    void shouldReturnExistingOperationWithoutApplyingItAgain() {
        SyncOperationRequest request = new SyncOperationRequest();
        request.setOperationId("operation-1");
        request.setRecordId(25L);
        request.setDeviceId("device-a");
        request.setOperationType("UPDATE");
        request.setPayload(Map.of("address", "New Address"));

        Operation existingOperation = new Operation();
        existingOperation.setOperationId("operation-1");
        existingOperation.setRecordId(25L);

        when(operationRepository.findByOperationId("operation-1"))
                .thenReturn(Optional.of(existingOperation));

        SyncOperationResponse response = syncService.processOperation(request);

        assertTrue(response.isSuccess());
        assertEquals(25L, response.getRecordId());
        assertEquals("Operation already processed", response.getMessage());

        verify(recordRepository, never()).findById(any());
        verify(recordRepository, never()).save(any(Record.class));
        verify(operationRepository, never()).save(any(Operation.class));
    }

    @Test
    void shouldBufferOutOfOrderOperationAndApplyItAfterMissingSequenceArrives() {
        String deviceId = "device-a";

        Operation sequenceOne = createOperation("operation-1", deviceId, 1L);

        SyncOperationRequest sequenceThree = createUpdateRequest(
                "operation-3",
                25L,
                deviceId,
                3L,
                "Address From Sequence 3"
        );

        when(operationRepository.findByOperationId("operation-3"))
                .thenReturn(Optional.empty());
        when(pendingOperationRepository.findByOperationId("operation-3"))
                .thenReturn(Optional.empty());
        when(operationRepository.findTopByDeviceIdOrderBySequenceNumberDesc(deviceId))
                .thenReturn(Optional.of(sequenceOne));

        SyncOperationResponse bufferedResponse =
                syncService.processOperation(sequenceThree);

        assertTrue(bufferedResponse.isSuccess());
        assertEquals(
                "Operation buffered. Waiting for sequence 2",
                bufferedResponse.getMessage()
        );

        verify(pendingOperationRepository).save(any(PendingOperation.class));
        verify(recordRepository, never()).findById(25L);

        PendingOperation bufferedOperation = new PendingOperation();
        bufferedOperation.setOperationId("operation-3");
        bufferedOperation.setRecordId(25L);
        bufferedOperation.setDeviceId(deviceId);
        bufferedOperation.setOperationType("UPDATE");
        bufferedOperation.setPayload(
                "{\"address\":\"Address From Sequence 3\"}"
        );
        bufferedOperation.setSequenceNumber(3L);
        bufferedOperation.setCreatedAt(System.currentTimeMillis());

        SyncOperationRequest sequenceTwo = createUpdateRequest(
                "operation-2",
                25L,
                deviceId,
                2L,
                "Address From Sequence 2"
        );

        Record record = createRecord(25L, "Old Address");

        Operation sequenceTwoOperation =
                createOperation("operation-2", deviceId, 2L);
        Operation sequenceThreeOperation =
                createOperation("operation-3", deviceId, 3L);

        when(operationRepository.findByOperationId("operation-2"))
                .thenReturn(Optional.empty());
        when(pendingOperationRepository.findByOperationId("operation-2"))
                .thenReturn(Optional.empty());
        when(operationRepository.findTopByDeviceIdOrderBySequenceNumberDesc(deviceId))
                .thenReturn(
                        Optional.of(sequenceOne),
                        Optional.of(sequenceOne),
                        Optional.of(sequenceTwoOperation),
                        Optional.of(sequenceThreeOperation)
                );
        when(recordRepository.findById(25L)).thenReturn(Optional.of(record));
        when(recordRepository.save(any(Record.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(operationRepository.save(any(Operation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingOperationRepository.findByDeviceIdAndSequenceNumber(
                deviceId, 3L
        )).thenReturn(Optional.of(bufferedOperation));

        SyncOperationResponse sequenceTwoResponse =
                syncService.processOperation(sequenceTwo);

        assertTrue(sequenceTwoResponse.isSuccess());
        assertEquals("Address From Sequence 3", record.getAddress());

        verify(pendingOperationRepository).delete(bufferedOperation);
        verify(recordRepository, times(2)).save(any(Record.class));
        verify(operationRepository, times(2)).save(any(Operation.class));
    }

    private SyncOperationRequest createUpdateRequest(
            String operationId,
            Long recordId,
            String deviceId,
            Long sequenceNumber,
            String address
    ) {
        SyncOperationRequest request = new SyncOperationRequest();
        request.setOperationId(operationId);
        request.setRecordId(recordId);
        request.setDeviceId(deviceId);
        request.setOperationType("UPDATE");
        request.setPayload(Map.of("address", address));
        request.setSequenceNumber(sequenceNumber);
        return request;
    }

    private Operation createOperation(
            String operationId,
            String deviceId,
            Long sequenceNumber
    ) {
        Operation operation = new Operation();
        operation.setOperationId(operationId);
        operation.setDeviceId(deviceId);
        operation.setSequenceNumber(sequenceNumber);
        return operation;
    }

    private Record createRecord(Long id, String address) {
        Record record = new Record();
        record.setId(id);
        record.setAddress(address);
        return record;
    }
}