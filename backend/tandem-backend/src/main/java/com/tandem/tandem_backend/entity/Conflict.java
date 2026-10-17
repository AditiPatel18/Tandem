package com.tandem.tandem_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conflicts")
public class Conflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recordId;

    @Column(nullable = false)
    private String incomingOperationId;

    @Column(nullable = false)
    private String existingOperationId;

    @Column(nullable = false)
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String incomingPayload;

    @Column(columnDefinition = "TEXT")
    private String existingPayload;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Conflict() {
    }

    public Long getId() {
        return id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getIncomingOperationId() {
        return incomingOperationId;
    }

    public void setIncomingOperationId(String incomingOperationId) {
        this.incomingOperationId = incomingOperationId;
    }

    public String getExistingOperationId() {
        return existingOperationId;
    }

    public void setExistingOperationId(String existingOperationId) {
        this.existingOperationId = existingOperationId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getIncomingPayload() {
        return incomingPayload;
    }

    public void setIncomingPayload(String incomingPayload) {
        this.incomingPayload = incomingPayload;
    }

    public String getExistingPayload() {
        return existingPayload;
    }

    public void setExistingPayload(String existingPayload) {
        this.existingPayload = existingPayload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}