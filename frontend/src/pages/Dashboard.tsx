import { useEffect, useState } from "react";
import { getRecords } from "../services/api";
import { deleteLocalRecord } from "../services/recordService";
import { syncPendingOperations } from "../services/syncService";

import type { RecordData } from "../types/record";

import RecordForm from "../components/RecordForm";
import EditRecordForm from "../components/EditRecordForm";
import { useNetworkSync } from "../hooks/useNetworkSync";

function Dashboard() {
  const [records, setRecords] = useState<RecordData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editingRecord, setEditingRecord] =
    useState<RecordData | null>(null);

  useNetworkSync();

  useEffect(() => {
  void loadRecords();
}, []);

  async function loadRecords() {
    try {
      setError("");

      const data = await getRecords();

      setRecords(data);
    } catch {
      setError("Failed to load records");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    const confirmed = window.confirm(
      "Are you sure you want to delete this record?"
    );

    if (!confirmed) return;

    try {
      setError("");

      await deleteLocalRecord(id);

      setEditingRecord(null);

      await loadRecords();
    } catch {
      setError("Failed to delete record");
    }
  }

  async function handleSync() {
    try {
      setError("");

      await syncPendingOperations();

      await loadRecords();
    } catch {
      setError("Synchronization failed");
    }
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <h1 className="text-3xl font-bold text-gray-800">
        Tandem Dashboard
      </h1>

      <p className="mt-2 text-gray-600">
        Offline-First Data Synchronization Engine
      </p>

      <button
        onClick={handleSync}
        className="mt-4 rounded bg-green-600 px-5 py-2 font-medium text-white hover:bg-green-700"
      >
        Sync Pending Changes
      </button>

      <div className="mt-8">
        <RecordForm onRecordCreated={loadRecords} />
      </div>

      {loading && (
        <p className="text-gray-600">
          Loading records...
        </p>
      )}

      {error && (
        <p className="mt-4 text-red-600">
          {error}
        </p>
      )}

      {!loading && !error && (
        <div className="space-y-4">
          {records.map((record) => (
            <div
              key={record.id}
              className="rounded-lg bg-white p-5 shadow"
            >
              <h2 className="text-xl font-semibold">
                {record.name}
              </h2>

              <p className="mt-1 text-gray-600">
                Phone: {record.phone}
              </p>

              <p className="text-gray-600">
                Village: {record.village}
              </p>

              <p className="text-gray-600">
                Address: {record.address}
              </p>

              <p className="mt-2 font-medium">
                Status: {record.status}
              </p>

              <button
                onClick={() => setEditingRecord(record)}
                className="mt-4 rounded bg-yellow-500 px-4 py-2 text-white hover:bg-yellow-600"
              >
                Edit
              </button>

              <button
                onClick={() => handleDelete(record.id)}
                className="ml-3 rounded bg-red-600 px-4 py-2 text-white hover:bg-red-700"
              >
                Delete
              </button>

              {editingRecord?.id === record.id && (
                <EditRecordForm
                  record={record}
                  onUpdated={() => {
                    setEditingRecord(null);
                    loadRecords();
                  }}
                  onCancel={() => setEditingRecord(null)}
                />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Dashboard;