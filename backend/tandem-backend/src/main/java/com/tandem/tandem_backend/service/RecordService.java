package com.tandem.tandem_backend.service;

import com.tandem.tandem_backend.entity.Record;
import com.tandem.tandem_backend.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Objects;
import com.tandem.tandem_backend.dto.UpdateRecordRequest;
import com.tandem.tandem_backend.entity.Operation;
import com.tandem.tandem_backend.repository.OperationRepository;

import java.util.UUID;

import java.util.List;
import java.util.Optional;

@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final OperationRepository operationRepository;

    @Autowired
    public RecordService(
            RecordRepository recordRepository,
            OperationRepository operationRepository) {

        this.recordRepository = recordRepository;
        this.operationRepository = operationRepository;
    }

    // Create a record
    public Record createRecord(Record record) {
        return recordRepository.save(record);
    }

    // Get all records
    public List<Record> getAllRecords() {
        return recordRepository.findAll();
    }

    // Get record by ID
    public Optional<Record> getRecordById(Long id) {
        return recordRepository.findById(id);
    }

    // Delete record
    public void deleteRecord(Long id) {
        recordRepository.deleteById(id);
    }

    public Optional<Record> updateRecord(
            Long id,
            UpdateRecordRequest request) {

        return recordRepository.findById(id)
                .map(existingRecord -> {

                    // Check name
                    if (!Objects.equals(
                            existingRecord.getName(),
                            request.getName())) {

                        createOperation(
                                id,
                                request.getDeviceId(),
                                "name",
                                existingRecord.getName(),
                                request.getName()
                        );

                        existingRecord.setName(request.getName());
                    }

                    // Check phone
                    if (!Objects.equals(
                            existingRecord.getPhone(),
                            request.getPhone())) {

                        createOperation(
                                id,
                                request.getDeviceId(),
                                "phone",
                                existingRecord.getPhone(),
                                request.getPhone()
                        );

                        existingRecord.setPhone(request.getPhone());
                    }

                    // Check village
                    if (!Objects.equals(
                            existingRecord.getVillage(),
                            request.getVillage())) {

                        createOperation(
                                id,
                                request.getDeviceId(),
                                "village",
                                existingRecord.getVillage(),
                                request.getVillage()
                        );

                        existingRecord.setVillage(request.getVillage());
                    }

                    // Check address
                    if (!Objects.equals(
                            existingRecord.getAddress(),
                            request.getAddress())) {

                        createOperation(
                                id,
                                request.getDeviceId(),
                                "address",
                                existingRecord.getAddress(),
                                request.getAddress()
                        );

                        existingRecord.setAddress(request.getAddress());
                    }

                    // Check status
                    if (!Objects.equals(
                            existingRecord.getStatus(),
                            request.getStatus())) {

                        createOperation(
                                id,
                                request.getDeviceId(),
                                "status",
                                existingRecord.getStatus(),
                                request.getStatus()
                        );

                        existingRecord.setStatus(request.getStatus());
                    }

                    return recordRepository.save(existingRecord);
                });
    }

    private void createOperation(
            Long recordId,
            String deviceId,
            String fieldName,
            String oldValue,
            String newValue) {

        Operation operation = new Operation();

        operation.setOperationId(UUID.randomUUID().toString());
        operation.setRecordId(recordId);
        operation.setDeviceId(deviceId);
        operation.setOperationType("UPDATE");
        operation.setFieldName(fieldName);
        operation.setOldValue(oldValue);
        operation.setNewValue(newValue);

        operationRepository.save(operation);
    }
}