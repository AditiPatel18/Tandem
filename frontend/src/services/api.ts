import type { RecordData } from "../types/record";
import {
  getLocalRecords,
  saveLocalRecords,
} from "./localRecordService";
import { getPendingOperations } from "./localOperationService";

const API_BASE_URL = "http://localhost:8080/api";

/*
 * GET RECORDS
 *
 * Online:
 * 1. Get latest records from server.
 * 2. Get local records.
 * 3. Get unsynchronized operations.
 * 4. Preserve local changes that are not synced yet.
 * 5. Save the merged result to IndexedDB.
 *
 * Offline:
 * → Use IndexedDB directly.
 */
export async function getRecords(): Promise<RecordData[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/records`);

    if (!response.ok) {
      throw new Error("Failed to fetch records");
    }

    const serverRecords: RecordData[] = await response.json();

    // Get current local records
    const localRecords = await getLocalRecords();

    // Get operations which are not successfully synchronized yet
    const pendingOperations = (
      await getPendingOperations()
    ).filter(
      (operation) =>
        operation.status === "PENDING" ||
        operation.status === "FAILED"
    );

    /*
     * Start with server data.
     *
     * Map makes it easy to replace a server record
     * with its local unsynchronized version.
     */
    const mergedRecords = new Map<number, RecordData>();

    for (const record of serverRecords) {
      mergedRecords.set(record.id, record);
    }

    /*
     * Preserve local changes which have not synced yet.
     */
    for (const operation of pendingOperations) {
      const localRecord = localRecords.find(
        (record) => record.id === operation.recordId
      );

      /*
       * DELETE operation:
       * The user deleted this record locally,
       * so it should not appear in the dashboard.
       */
      if (operation.operationType === "DELETE") {
        mergedRecords.delete(operation.recordId);
        continue;
      }

      /*
       * CREATE / UPDATE:
       * Preserve the local version.
       */
      if (localRecord) {
        mergedRecords.set(localRecord.id, localRecord);
      }
    }

    const finalRecords = Array.from(mergedRecords.values());

    // Store the safely merged result locally
    await saveLocalRecords(finalRecords);

    return finalRecords;
  } catch {
    /*
     * Backend unavailable.
     *
     * IndexedDB becomes the source of truth for
     * the current offline session.
     */
    return await getLocalRecords();
  }
}

/*
 * ONLINE CREATE
 *
 * Kept for normal online API usage.
 */
export async function createRecord(
  record: Omit<RecordData, "id">
): Promise<RecordData> {
  const response = await fetch(`${API_BASE_URL}/records`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(record),
  });

  if (!response.ok) {
    throw new Error("Failed to create record");
  }

  return response.json();
}

/*
 * ONLINE UPDATE
 *
 * Kept for normal online API usage.
 */
export async function updateRecord(
  id: number,
  record: Omit<RecordData, "id"> & { deviceId: string }
): Promise<RecordData> {
  const response = await fetch(`${API_BASE_URL}/records/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(record),
  });

  if (!response.ok) {
    throw new Error("Failed to update record");
  }

  return response.json();
}

/*
 * ONLINE DELETE
 *
 * Kept for normal online API usage.
 */
export async function deleteRecord(id: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/records/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error("Failed to delete record");
  }
}