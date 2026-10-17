package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Conflict;
import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.entity.Record;
import com.tandem.tandem_backend.repository.ConflictRepository;
import com.tandem.tandem_backend.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConflictServiceTest {

    @Mock
    private ConflictRepository conflictRepository;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private ConflictService conflictService;

    private Conflict conflict;
    private Record record;

    @BeforeEach
    void setUp() {
        conflict = new Conflict();
        conflict.setRecordId(1L);
        conflict.setIncomingOperationId("incoming-1");
        conflict.setExistingOperationId("existing-1");
        conflict.setIncomingPayload("{\"name\":\"Incoming Name\"}");
        conflict.setExistingPayload("{\"name\":\"Existing Name\"}");
        conflict.setStatus("OPEN");

        record = new Record(
                "Original Name",
                "1234567890",
                "Village A",
                "Address A",
                "Active"
        );
    }

    @Test
    void createConflictShouldSaveOpenConflict() {
        Operation operation = mock(Operation.class);

        when(operation.getOperationId()).thenReturn("existing-1");
        when(operation.getPayload())
                .thenReturn("{\"name\":\"Existing Name\"}");

        when(conflictRepository.save(any(Conflict.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conflict result = conflictService.createConflict(
                1L,
                operation,
                "incoming-1",
                "{\"name\":\"Incoming Name\"}"
        );

        assertEquals(1L, result.getRecordId());
        assertEquals("OPEN", result.getStatus());
        assertEquals("incoming-1", result.getIncomingOperationId());
        assertEquals("existing-1", result.getExistingOperationId());

        verify(conflictRepository).save(any(Conflict.class));
    }

    @Test
    void getAllConflictsShouldReturnRepositoryResults() {
        when(conflictRepository.findAll())
                .thenReturn(java.util.List.of(conflict));

        assertEquals(1, conflictService.getAllConflicts().size());

        verify(conflictRepository).findAll();
    }

    @Test
    void getOpenConflictsShouldReturnOpenConflicts() {
        when(conflictRepository.findByStatusOrderByCreatedAtAsc("OPEN"))
                .thenReturn(java.util.List.of(conflict));

        assertEquals(1, conflictService.getOpenConflicts().size());

        verify(conflictRepository)
                .findByStatusOrderByCreatedAtAsc("OPEN");
    }

    @Test
    void getConflictsForRecordShouldReturnRecordConflicts() {
        when(conflictRepository.findByRecordIdOrderByCreatedAtAsc(1L))
                .thenReturn(java.util.List.of(conflict));

        assertEquals(
                1,
                conflictService.getConflictsForRecord(1L).size()
        );

        verify(conflictRepository)
                .findByRecordIdOrderByCreatedAtAsc(1L);
    }

    @Test
    void resolveConflictShouldRejectUnknownConflict() {
        when(conflictRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.resolveConflict(
                        99L,
                        "KEEP_INCOMING"
                )
        );

        verify(recordRepository, never()).save(any(Record.class));
    }

    @Test
    void resolveConflictShouldRejectAlreadyResolvedConflict() {
        conflict.setStatus("RESOLVED");

        when(conflictRepository.findById(1L))
                .thenReturn(Optional.of(conflict));

        assertThrows(
                IllegalStateException.class,
                () -> conflictService.resolveConflict(
                        1L,
                        "KEEP_INCOMING"
                )
        );

        verify(recordRepository, never()).save(any(Record.class));
    }

    @Test
    void resolveConflictShouldRejectInvalidResolution() {
        when(conflictRepository.findById(1L))
                .thenReturn(Optional.of(conflict));

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.resolveConflict(
                        1L,
                        "INVALID"
                )
        );

        verify(recordRepository, never()).save(any(Record.class));
    }

    @Test
    void resolveConflictShouldRejectMissingRecord() {
        when(conflictRepository.findById(1L))
                .thenReturn(Optional.of(conflict));

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.resolveConflict(
                        1L,
                        "KEEP_INCOMING"
                )
        );

        verify(recordRepository, never()).save(any(Record.class));
    }
}