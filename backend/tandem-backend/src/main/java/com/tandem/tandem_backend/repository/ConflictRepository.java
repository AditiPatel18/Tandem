package com.tandem.tandem_backend.repository;

import com.tandem.tandem_backend.entity.Conflict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConflictRepository extends JpaRepository<Conflict, Long> {

    List<Conflict> findByRecordIdOrderByCreatedAtAsc(Long recordId);

    List<Conflict> findByStatusOrderByCreatedAtAsc(String status);

    Optional<Conflict>
    findFirstByRecordIdAndExistingOperationIdAndIncomingOperationIdAndStatus(
            Long recordId,
            String existingOperationId,
            String incomingOperationId,
            String status
    );

    Optional<Conflict>
    findFirstByRecordIdAndFieldNameAndStatus(
            Long recordId,
            String fieldName,
            String status
    );
}