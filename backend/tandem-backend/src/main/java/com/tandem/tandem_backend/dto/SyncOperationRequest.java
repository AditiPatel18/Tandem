package com.tandem.tandem_backend.dto;

import java.time.Instant;
import java.util.Map;

public class SyncOperationRequest {

    private String operationId;
    private Long recordId;
    private String deviceId;
    private String operationType;
    private Map<String, Object> payload;
    private Instant createdAt;
    private Long sequenceNumber;
    private Map<String, Long> causalContext;

    public SyncOperationRequest() {
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Map<String, Long> getCausalContext() {
        return causalContext;
    }

    public void setCausalContext(
            Map<String, Long> causalContext
    ) {
        this.causalContext = causalContext;
    }
}