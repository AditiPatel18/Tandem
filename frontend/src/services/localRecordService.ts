import { db } from "../db/database";
import type { RecordData } from "../types/record";

export async function getLocalRecords(): Promise<RecordData[]> {
  return await db.records.toArray();
}

export async function saveLocalRecords(
  records: RecordData[]
): Promise<void> {
  await db.transaction("rw", db.records, async () => {
    await db.records.clear();
    await db.records.bulkPut(records);
  });
}

export async function createLocalRecord(
  record: Omit<RecordData, "id">
): Promise<RecordData> {
  const temporaryId = -(Date.now() * 1000 + Math.floor(Math.random() * 1000));

  const localRecord: RecordData = {
    id: temporaryId,
    ...record,
  };

  await db.records.put(localRecord);

  return localRecord;
}

export async function updateLocalRecord(
  id: number,
  record: Omit<RecordData, "id">
): Promise<void> {
  const existingRecord = await db.records.get(id);

  if (!existingRecord) {
    throw new Error("Record not found locally");
  }

  await db.records.put({
    id,
    ...record,
  });
}

export async function deleteLocalRecord(
  id: number
): Promise<void> {
  await db.records.delete(id);
}

export async function replaceLocalRecordId(
  localId: number,
  serverId: number
): Promise<void> {
  await db.transaction(
    "rw",
    db.records,
    db.operations,
    async () => {
      const record = await db.records.get(localId);

      if (record) {
        await db.records.delete(localId);

        await db.records.put({
          ...record,
          id: serverId,
        });
      }

      await db.operations
        .where("recordId")
        .equals(localId)
        .modify({
          recordId: serverId,
        });
    }
  );
}