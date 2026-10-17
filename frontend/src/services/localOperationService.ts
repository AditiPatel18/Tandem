import { db } from "../db/database";
import type { Operation } from "../types/operation";

export async function addOperation(
  operation: Operation
): Promise<number> {
  return await db.operations.add(operation);
}

export async function getPendingOperations(): Promise<Operation[]> {
  const operations = await db.operations.toArray();

  return operations
    .filter(
      (operation) =>
        operation.status === "PENDING" ||
        operation.status === "FAILED"
    )
    .sort(
      (a, b) =>
        a.createdAt - b.createdAt
    );
}

export async function markOperationSynced(
  operationId: string
): Promise<void> {
  await db.operations
    .where("operationId")
    .equals(operationId)
    .modify({
      status: "SYNCED",
    });
}

export async function markOperationFailed(
  operationId: string
): Promise<void> {
  await db.operations
    .where("operationId")
    .equals(operationId)
    .modify({
      status: "FAILED",
    });
}

export async function updatePendingRecordId(
  oldRecordId: number,
  newRecordId: number
): Promise<void> {
  await db.operations
    .where("recordId")
    .equals(oldRecordId)
    .modify({
      recordId: newRecordId,
    });
}

export async function deleteOperation(
  operationId: string
): Promise<void> {
  await db.operations
    .where("operationId")
    .equals(operationId)
    .delete();
}

export function getNextSequenceNumber(
  deviceId: string
): number {
  const key =
    `tandem_sequence_number_${deviceId}`;

  const currentValue = Number(
    localStorage.getItem(key) || "0"
  );

  const nextValue =
    currentValue + 1;

  localStorage.setItem(
    key,
    String(nextValue)
  );

  return nextValue;
}