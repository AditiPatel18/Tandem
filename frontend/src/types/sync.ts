export interface SyncOperationRequest {
  operationId: string;
  recordId: number;
  deviceId: string;
  operationType: "CREATE" | "UPDATE" | "DELETE";
  payload: Record<string, unknown>;
  createdAt: number;
  sequenceNumber: number;
  causalContext: Record<string, number>;
}

export type SyncOperationStatus =
  | "APPLIED"
  | "BUFFERED"
  | "DUPLICATE"
  | "FAILED";

export interface SyncOperationResponse {
  operationId: string | null;
  success: boolean;
  recordId: number | null;
  message: string;
  status: SyncOperationStatus;
}