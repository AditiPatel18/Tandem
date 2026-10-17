package com.tandem.tandem_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "operations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "operationId")
        }
)
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String operationId;

    private Long recordId;

    private String deviceId;

    private String operationType;

    private String fieldName;

    private String oldValue;

    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Long sequenceNumber;

    @Column(columnDefinition = "TEXT")
    private String causalContext;

    public Operation() {
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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
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
}