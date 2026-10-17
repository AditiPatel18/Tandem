export interface Operation {
  id?: number;

  operationId: string;

  recordId: number;

  deviceId: string;

  operationType: "CREATE" | "UPDATE" | "DELETE";

  payload: Record<string, unknown>;

  createdAt: number;

  sequenceNumber: number;

  causalContext: Record<string, number>;

  status: "PENDING" | "SYNCED" | "FAILED";
}