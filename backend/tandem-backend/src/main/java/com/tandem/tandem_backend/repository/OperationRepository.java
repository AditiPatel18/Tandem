package com.tandem.tandem_backend.repository;

import com.tandem.tandem_backend.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    Optional<Operation> findByOperationId(String operationId);

    Optional<Operation> findTopByDeviceIdOrderBySequenceNumberDesc(
            String deviceId
    );

    List<Operation> findByRecordIdOrderBySequenceNumberAsc(Long recordId);
}