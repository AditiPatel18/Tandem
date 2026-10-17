package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.repository.OperationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OperationService {

    private final OperationRepository operationRepository;

    public OperationService(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    public Operation createOperation(Operation operation) {

        Optional<Operation> existingOperation =
                operationRepository.findByOperationId(
                        operation.getOperationId()
                );

        if (existingOperation.isPresent()) {
            return existingOperation.get();
        }

        return operationRepository.save(operation);
    }

    public List<Operation> getAllOperations() {
        return operationRepository.findAll();
    }

    public Optional<Operation> getOperationById(Long id) {
        return operationRepository.findById(id);
    }

    public Optional<Operation> getOperationByOperationId(String operationId) {
        return operationRepository.findByOperationId(operationId);
    }
}