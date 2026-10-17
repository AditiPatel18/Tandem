import Dexie, { type Table } from "dexie";
import type { RecordData } from "../types/record";
import type { Operation } from "../types/operation";

export class TandemDatabase extends Dexie {
  records!: Table<RecordData, number>;
  operations!: Table<Operation, number>;

  constructor() {
    super("TandemDatabase");

    this.version(2).stores({
      records:
        "++id, name, phone, village, status",

      operations:
        "++id, &operationId, recordId, deviceId, operationType, status, createdAt",
    });

    this.version(3)
      .stores({
        records:
          "++id, name, phone, village, status",

        operations:
          "++id, &operationId, recordId, deviceId, operationType, status, createdAt",
      })
      .upgrade(async (transaction) => {
        await transaction
          .table<Operation, number>("operations")
          .toCollection()
          .modify((operation) => {
            if (!operation.causalContext) {
              operation.causalContext = {};
            }
          });
      });
  }
}

export const db = new TandemDatabase();