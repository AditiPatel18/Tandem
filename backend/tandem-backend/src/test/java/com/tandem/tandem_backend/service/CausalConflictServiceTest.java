package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.repository.OperationRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CausalConflictServiceTest {

    @Test
    void shouldFindConcurrentOperation() {
        OperationRepository repository =
                mock(OperationRepository.class);

        JsonMapper jsonMapper =
                JsonMapper.builder().build();

        CausalConflictService service =
                new CausalConflictService(
                        repository,
                        jsonMapper
                );

        Operation operation = new Operation();

        operation.setOperationId("existing-operation");
        operation.setRecordId(23L);
        operation.setDeviceId("device-A");
        operation.setSequenceNumber(2L);
        operation.setCausalContext(
                "{\"device-A\":2,\"device-B\":1}"
        );

        when(
                repository.findByRecordIdOrderBySequenceNumberAsc(23L)
        ).thenReturn(List.of(operation));

        Map<String, Long> incomingContext = Map.of(
                "device-A", 1L,
                "device-B", 2L
        );

        List<Operation> conflicts =
                service.findConcurrentOperations(
                        23L,
                        incomingContext,
                        "device-B",
                        2L
                );

        assertEquals(1, conflicts.size());

        assertEquals(
                "existing-operation",
                conflicts.get(0).getOperationId()
        );
    }

    @Test
    void shouldReturnEmptyWhenNoConcurrentOperationExists() {
        OperationRepository repository =
                mock(OperationRepository.class);

        JsonMapper jsonMapper =
                JsonMapper.builder().build();

        CausalConflictService service =
                new CausalConflictService(
                        repository,
                        jsonMapper
                );

        Operation operation = new Operation();

        operation.setOperationId("existing-operation");
        operation.setRecordId(23L);
        operation.setDeviceId("device-A");
        operation.setSequenceNumber(2L);
        operation.setCausalContext(
                "{\"device-A\":2,\"device-B\":1}"
        );

        when(
                repository.findByRecordIdOrderBySequenceNumberAsc(23L)
        ).thenReturn(List.of(operation));

        Map<String, Long> incomingContext = Map.of(
                "device-A", 3L,
                "device-B", 1L
        );

        List<Operation> conflicts =
                service.findConcurrentOperations(
                        23L,
                        incomingContext,
                        "device-A",
                        3L
                );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldDetectConcurrentOperationsWhenBothContextsAreEmpty() {
        OperationRepository repository =
                mock(OperationRepository.class);

        JsonMapper jsonMapper =
                JsonMapper.builder().build();

        CausalConflictService service =
                new CausalConflictService(
                        repository,
                        jsonMapper
                );

        Operation operation = new Operation();

        operation.setOperationId("existing-operation");
        operation.setRecordId(26L);
        operation.setDeviceId("device-A");
        operation.setSequenceNumber(1L);
        operation.setCausalContext("{}");

        when(
                repository.findByRecordIdOrderBySequenceNumberAsc(26L)
        ).thenReturn(List.of(operation));

        Map<String, Long> incomingContext = Map.of();

        List<Operation> conflicts =
                service.findConcurrentOperations(
                        26L,
                        incomingContext,
                        "device-B",
                        1L
                );

        assertEquals(1, conflicts.size());

        assertEquals(
                "existing-operation",
                conflicts.get(0).getOperationId()
        );
    }
}