package com.tandem.tandem_backend.repository;

import com.tandem.tandem_backend.entity.PendingOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingOperationRepository
        extends JpaRepository<PendingOperation, Long> {

    Optional<PendingOperation> findByOperationId(
            String operationId
    );

    List<PendingOperation>
    findByDeviceIdOrderBySequenceNumberAsc(
            String deviceId
    );

    Optional<PendingOperation>
    findByDeviceIdAndSequenceNumber(
            String deviceId,
            Long sequenceNumber
    );
}