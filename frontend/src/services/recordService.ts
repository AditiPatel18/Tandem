import { db } from "../db/database";
import {
  addOperation,
  getNextSequenceNumber,
} from "./localOperationService";
import {
  getCausalContext,
  updateCausalContext,
} from "./causalContextService";
import type { RecordData } from "../types/record";

function generateOperationId(): string {
  return crypto.randomUUID();
}

function getDeviceId(): string {
  let deviceId =
    localStorage.getItem("tandem_device_id");

  if (!deviceId) {
    deviceId = crypto.randomUUID();

    localStorage.setItem(
      "tandem_device_id",
      deviceId
    );
  }

  return deviceId;
}

export async function createLocalRecord(
  record: Omit<RecordData, "id">
): Promise<RecordData> {
  const temporaryId = -Date.now();

  const localRecord: RecordData = {
    id: temporaryId,
    ...record,
  };

  await db.records.put(localRecord);

  const deviceId = getDeviceId();

  const causalContext =
    getCausalContext();

  const sequenceNumber =
    getNextSequenceNumber(deviceId);

  await addOperation({
    operationId: generateOperationId(),
    recordId: temporaryId,
    deviceId,
    operationType: "CREATE",
    payload: { ...record },
    createdAt: Date.now(),
    sequenceNumber,
    causalContext,
    status: "PENDING",
  });

  updateCausalContext(
    deviceId,
    sequenceNumber
  );

  return localRecord;
}

export async function getLocalRecords(): Promise<
  RecordData[]
> {
  return db.records.toArray();
}

export async function updateLocalRecord(
  id: number,
  record: Omit<RecordData, "id">
): Promise<RecordData> {
  const existingRecord =
    await db.records.get(id);

  if (!existingRecord) {
    throw new Error(
      "Record not found locally"
    );
  }

  const changedFields: Record<
    string,
    unknown
  > = {};

  for (const key of Object.keys(record) as Array<
    keyof Omit<RecordData, "id">
  >) {
    if (
      existingRecord[key] !==
      record[key]
    ) {
      changedFields[key] =
        record[key];
    }
  }

  const updatedRecord: RecordData = {
    id,
    ...record,
  };

  await db.records.put(updatedRecord);

  if (
    Object.keys(changedFields).length === 0
  ) {
    return updatedRecord;
  }

  const deviceId = getDeviceId();

  const causalContext =
    getCausalContext();

  const sequenceNumber =
    getNextSequenceNumber(deviceId);

  await addOperation({
    operationId: generateOperationId(),
    recordId: id,
    deviceId,
    operationType: "UPDATE",
    payload: changedFields,
    createdAt: Date.now(),
    sequenceNumber,
    causalContext,
    status: "PENDING",
  });

  updateCausalContext(
    deviceId,
    sequenceNumber
  );

  return updatedRecord;
}

export async function deleteLocalRecord(
  id: number
): Promise<void> {
  const existingRecord =
    await db.records.get(id);

  if (!existingRecord) {
    throw new Error(
      "Record not found locally"
    );
  }

  await db.records.delete(id);

  const deviceId = getDeviceId();

  const causalContext =
    getCausalContext();

  const sequenceNumber =
    getNextSequenceNumber(deviceId);

  await addOperation({
    operationId: generateOperationId(),
    recordId: id,
    deviceId,
    operationType: "DELETE",
    payload: {},
    createdAt: Date.now(),
    sequenceNumber,
    causalContext,
    status: "PENDING",
  });

  updateCausalContext(
    deviceId,
    sequenceNumber
  );
}