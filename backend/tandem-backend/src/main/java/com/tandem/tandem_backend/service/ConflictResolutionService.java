package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Operation;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Objects;

@Service
public class ConflictResolutionService {

    private final JsonMapper jsonMapper;

    public ConflictResolutionService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ResolutionResult resolve(
            Map<String, Object> incomingPayload,
            Operation existingOperation
    ) {
        if (existingOperation == null) {
            return new ResolutionResult(
                    ResolutionType.NO_CONFLICT,
                    incomingPayload,
                    null,
                    "No existing operation found"
            );
        }

        Map<String, Object> existingPayload =
                parseExistingPayload(existingOperation);

        for (String field : incomingPayload.keySet()) {

            if (!existingPayload.containsKey(field)) {
                continue;
            }

            Object incomingValue =
                    incomingPayload.get(field);

            Object existingValue =
                    existingPayload.get(field);

            if (!Objects.equals(
                    incomingValue,
                    existingValue
            )) {
                return new ResolutionResult(
                        ResolutionType.FIELD_CONFLICT,
                        incomingPayload,
                        field,
                        "Concurrent operations changed the same field: "
                                + field
                );
            }
        }

        return new ResolutionResult(
                ResolutionType.MERGE,
                incomingPayload,
                null,
                "Concurrent operations changed different fields"
        );
    }

    private Map<String, Object> parseExistingPayload(
            Operation operation
    ) {
        if (operation.getPayload() == null) {
            return Map.of();
        }

        try {
            return jsonMapper.readValue(
                    operation.getPayload(),
                    Map.class
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to read operation payload for "
                            + operation.getOperationId(),
                    exception
            );
        }
    }

    public enum ResolutionType {
        NO_CONFLICT,
        MERGE,
        FIELD_CONFLICT
    }

    public static class ResolutionResult {

        private final ResolutionType type;

        private final Map<String, Object> resolvedPayload;

        private final String conflictingField;

        private final String message;

        public ResolutionResult(
                ResolutionType type,
                Map<String, Object> resolvedPayload,
                String conflictingField,
                String message
        ) {
            this.type = type;
            this.resolvedPayload = resolvedPayload;
            this.conflictingField = conflictingField;
            this.message = message;
        }

        public ResolutionType getType() {
            return type;
        }

        public Map<String, Object> getResolvedPayload() {
            return resolvedPayload;
        }

        public String getConflictingField() {
            return conflictingField;
        }

        public String getMessage() {
            return message;
        }
    }
}