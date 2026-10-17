package com.tandem.tandem_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "pending_operations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "operationId")
        }
)
public class PendingOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String operationId;

    private Long recordId;

    private String deviceId;

    private String operationType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Long sequenceNumber;

    @Column(columnDefinition = "TEXT")
    private String causalContext;

    private Long createdAt;

    public PendingOperation() {
    }

    public Long getId() {
        return id;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getCausalContext() {
        return causalContext;
    }

    public void setCausalContext(String causalContext) {
        this.causalContext = causalContext;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
