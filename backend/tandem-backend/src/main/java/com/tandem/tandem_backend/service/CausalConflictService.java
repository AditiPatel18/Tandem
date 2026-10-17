package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.repository.OperationRepository;
import com.tandem.tandem_backend.sync.VersionVector;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CausalConflictService {

    private final OperationRepository operationRepository;
    private final JsonMapper jsonMapper;

    public CausalConflictService(
            OperationRepository operationRepository,
            JsonMapper jsonMapper
    ) {
        this.operationRepository = operationRepository;
        this.jsonMapper = jsonMapper;
    }

    public List<Operation> findConcurrentOperations(
            Long recordId,
            Map<String, Long> incomingCausalContext,
            String incomingDeviceId,
            Long incomingSequenceNumber
    ) {
        List<Operation> operations =
                operationRepository.findByRecordIdOrderBySequenceNumberAsc(
                        recordId
                );

        List<Operation> concurrentOperations =
                new ArrayList<>();

        Map<String, Long> incomingVersionVector =
                buildIncomingVersionVector(
                        incomingCausalContext,
                        incomingDeviceId,
                        incomingSequenceNumber
                );

        for (Operation operation : operations) {

            if (operation.getCausalContext() == null) {
                continue;
            }

            try {
                Map<String, Long> existingCausalContext =
                        jsonMapper.readValue(
                                operation.getCausalContext(),
                                new TypeReference<Map<String, Long>>() {
                                }
                        );

                Map<String, Long> existingVersionVector =
                        buildExistingVersionVector(
                                operation,
                                existingCausalContext
                        );

                VersionVector.Relation relation =
                        VersionVector.compare(
                                existingVersionVector,
                                incomingVersionVector
                        );

                if (relation ==
                        VersionVector.Relation.CONCURRENT) {

                    concurrentOperations.add(operation);
                }

            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Failed to read causal context for operation "
                                + operation.getOperationId(),
                        exception
                );
            }
        }

        return concurrentOperations;
    }

    private Map<String, Long> buildIncomingVersionVector(
            Map<String, Long> causalContext,
            String deviceId,
            Long sequenceNumber
    ) {
        Map<String, Long> versionVector =
                causalContext == null
                        ? new HashMap<>()
                        : new HashMap<>(causalContext);

        if (deviceId != null && sequenceNumber != null) {

            long currentSequence =
                    versionVector.getOrDefault(
                            deviceId,
                            0L
                    );

            if (sequenceNumber > currentSequence) {
                versionVector.put(
                        deviceId,
                        sequenceNumber
                );
            }
        }

        return versionVector;
    }

    private Map<String, Long> buildExistingVersionVector(
            Operation operation,
            Map<String, Long> causalContext
    ) {
        Map<String, Long> versionVector =
                causalContext == null
                        ? new HashMap<>()
                        : new HashMap<>(causalContext);

        if (operation.getDeviceId() != null
                && operation.getSequenceNumber() != null) {

            long currentSequence =
                    versionVector.getOrDefault(
                            operation.getDeviceId(),
                            0L
                    );

            if (operation.getSequenceNumber()
                    > currentSequence) {

                versionVector.put(
                        operation.getDeviceId(),
                        operation.getSequenceNumber()
                );
            }
        }

        return versionVector;
    }
}