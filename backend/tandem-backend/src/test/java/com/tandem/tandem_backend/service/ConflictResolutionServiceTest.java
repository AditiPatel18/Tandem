package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Operation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConflictResolutionServiceTest {

    private final ConflictResolutionService service =
            new ConflictResolutionService(
                    JsonMapper.builder().build()
            );

    @Test
    void shouldMergeWhenDifferentFieldsAreChanged() {

        Operation existingOperation =
                new Operation();

        existingOperation.setOperationId(
                "existing-operation"
        );

        existingOperation.setPayload(
                "{\"address\":\"Address A\"}"
        );

        Map<String, Object> incomingPayload =
                Map.of(
                        "phone",
                        "9999999999"
                );

        ConflictResolutionService.ResolutionResult result =
                service.resolve(
                        incomingPayload,
                        existingOperation
                );

        assertEquals(
                ConflictResolutionService.ResolutionType.MERGE,
                result.getType()
        );

        assertEquals(
                null,
                result.getConflictingField()
        );
    }

    @Test
    void shouldDetectConflictWhenSameFieldIsChanged() {

        Operation existingOperation =
                new Operation();

        existingOperation.setOperationId(
                "existing-operation"
        );

        existingOperation.setPayload(
                "{\"address\":\"Address A\"}"
        );

        Map<String, Object> incomingPayload =
                Map.of(
                        "address",
                        "Address B"
                );

        ConflictResolutionService.ResolutionResult result =
                service.resolve(
                        incomingPayload,
                        existingOperation
                );

        assertEquals(
                ConflictResolutionService.ResolutionType.FIELD_CONFLICT,
                result.getType()
        );

        assertEquals(
                "address",
                result.getConflictingField()
        );
    }
}