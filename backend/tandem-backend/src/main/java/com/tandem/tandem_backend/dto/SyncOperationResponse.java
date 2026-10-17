package com.tandem.tandem_backend.dto;

public class SyncOperationResponse {

    public enum Status {
        APPLIED,
        BUFFERED,
        DUPLICATE,
        FAILED
    }

    private String operationId;

    private boolean success;

    private Long recordId;

    private String message;

    private Status status;

    public SyncOperationResponse() {
    }

    public SyncOperationResponse(
            String operationId,
            boolean success,
            Long recordId,
            String message) {

        this(
                operationId,
                success,
                recordId,
                message,
                success
                        ? Status.APPLIED
                        : Status.FAILED
        );
    }

    public SyncOperationResponse(
            String operationId,
            boolean success,
            Long recordId,
            String message,
            Status status) {

        this.operationId = operationId;
        this.success = success;
        this.recordId = recordId;
        this.message = message;
        this.status = status;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}