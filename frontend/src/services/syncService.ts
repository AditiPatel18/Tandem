import { db } from "../db/database";
import {
  getPendingOperations,
  markOperationSynced,
  markOperationFailed,
  updatePendingRecordId,
} from "./localOperationService";
import { replaceLocalRecordId } from "./localRecordService";
import { sendWebSocketOperation } from "./webSocketService";
import type {
  SyncOperationRequest,
  SyncOperationResponse,
} from "../types/sync";

export async function syncPendingOperations(): Promise<void> {
  const operations = await getPendingOperations();

  for (const operation of operations) {
    try {
      const request: SyncOperationRequest = {
        operationId: operation.operationId,
        recordId: operation.recordId,
        deviceId: operation.deviceId,
        operationType: operation.operationType,
        payload: operation.payload,
        createdAt: operation.createdAt,
        sequenceNumber:
          operation.sequenceNumber ?? operation.createdAt,
        causalContext: operation.causalContext,
      };

      const result: SyncOperationResponse =
        await sendWebSocketOperation(request);

      if (!result.success) {
        throw new Error(
          result.message ||
            "Operation synchronization failed"
        );
      }

      if (result.status === "BUFFERED") {
        console.log(
          "Operation buffered:",
          operation.operationType,
          operation.operationId,
          result.message
        );

        continue;
      }

      if (
        (result.status === "APPLIED" ||
          result.status === "DUPLICATE") &&
        operation.operationType === "CREATE" &&
        result.recordId !== null &&
        result.recordId !== operation.recordId
      ) {
        const oldRecordId = operation.recordId;
        const newRecordId = result.recordId;

        await replaceLocalRecordId(
          oldRecordId,
          newRecordId
        );

        await updatePendingRecordId(
          oldRecordId,
          newRecordId
        );

        for (const pendingOperation of operations) {
          if (
            pendingOperation.recordId ===
            oldRecordId
          ) {
            pendingOperation.recordId =
              newRecordId;
          }
        }

        console.log(
          "Local record ID replaced:",
          oldRecordId,
          "→",
          newRecordId
        );
      }

      if (
        result.status === "APPLIED" ||
        result.status === "DUPLICATE"
      ) {
        await markOperationSynced(
          operation.operationId
        );

        const updatedOperation =
          await db.operations
            .where("operationId")
            .equals(operation.operationId)
            .first();

        console.log(
          "Synchronized:",
          operation.operationType,
          operation.operationId,
          updatedOperation
        );
      }
    } catch (error) {
      console.error(
        "SYNC ERROR:",
        operation.operationId,
        error
      );

      await markOperationFailed(
        operation.operationId
      );
    }
  }
}