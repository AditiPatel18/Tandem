import { useCallback, useEffect, useState } from "react";

import {
  getOpenConflicts,
  resolveConflict,
} from "../services/conflictService";

import type {
  Conflict,
  ConflictResolution,
} from "../services/conflictService";

function parsePayload(payload: string): Record<string, unknown> {
  try {
    const parsed: unknown = JSON.parse(payload);

    if (
      parsed !== null &&
      typeof parsed === "object" &&
      !Array.isArray(parsed)
    ) {
      return parsed as Record<string, unknown>;
    }
  } catch {
    // Keep the page usable if a stored payload is malformed.
  }

  return {};
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return "—";
  }

  if (typeof value === "object") {
    return JSON.stringify(value);
  }

  return String(value);
}

function Conflicts() {
  const [conflicts, setConflicts] = useState<Conflict[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [success, setSuccess] = useState("");

  const loadConflicts = useCallback(async () => {
    try {
      setError("");

      const data = await getOpenConflicts();

      setConflicts(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to load conflicts"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadConflicts();
  }, [loadConflicts]);

  async function handleResolve(
    conflictId: number,
    resolution: ConflictResolution
  ) {
    const confirmed = window.confirm(
      resolution === "KEEP_INCOMING"
        ? "Resolve this conflict by keeping the incoming changes?"
        : "Resolve this conflict by keeping the existing changes?"
    );

    if (!confirmed) return;

    try {
      setResolvingId(conflictId);
      setError("");
      setSuccess("");

      await resolveConflict(conflictId, resolution);

      setSuccess(`Conflict #${conflictId} resolved successfully.`);

      await loadConflicts();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to resolve conflict"
      );
    } finally {
      setResolvingId(null);
    }
  }

  return (
    <main className="min-h-screen bg-gray-100 px-6 py-8 md:px-10">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800">
            Conflict Resolution
          </h1>

          <p className="mt-2 text-gray-600">
            Review concurrent edits and choose which changes to keep.
          </p>
        </div>

        <div className="mb-6 rounded-lg bg-white p-5 shadow-sm">
          <p className="text-sm text-gray-500">
            Unresolved conflicts
          </p>

          <p className="mt-1 text-3xl font-bold text-indigo-600">
            {conflicts.length}
          </p>
        </div>

        {success && (
          <div className="mb-5 rounded-lg border border-green-200 bg-green-50 p-4 text-green-700">
            {success}
          </div>
        )}

        {error && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <p className="py-8 text-gray-600">
            Loading conflicts...
          </p>
        ) : conflicts.length === 0 && !error ? (
          <div className="rounded-xl bg-white p-10 text-center shadow-sm">
            <h2 className="text-xl font-semibold text-gray-800">
              No unresolved conflicts
            </h2>

            <p className="mt-2 text-gray-600">
              Your synchronized records are up to date.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {conflicts.map((conflict) => {
              const existing = parsePayload(
                conflict.existingPayload
              );

              const incoming = parsePayload(
                conflict.incomingPayload
              );

              const fields = Array.from(
                new Set([
                  ...Object.keys(existing),
                  ...Object.keys(incoming),
                ])
              );

              return (
                <section
                  key={conflict.id}
                  className="rounded-xl bg-white p-6 shadow-sm"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h2 className="text-xl font-semibold text-gray-800">
                        Conflict #{conflict.id}
                      </h2>

                      <p className="mt-1 text-sm text-gray-500">
                        Record ID: {conflict.recordId}
                      </p>
                    </div>

                    <span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-800">
                      {conflict.status}
                    </span>
                  </div>

                  <div className="mt-5 grid gap-4 md:grid-cols-2">
                    <div className="rounded-lg border border-blue-200 p-4">
                      <h3 className="font-semibold text-blue-700">
                        Existing changes
                      </h3>

                      <p className="mt-1 break-all text-xs text-gray-500">
                        {conflict.existingOperationId}
                      </p>

                      <div className="mt-4 space-y-3">
                        {Object.keys(existing).length === 0 ? (
                          <p className="text-sm text-gray-500">
                            No payload fields.
                          </p>
                        ) : (
                          Object.entries(existing).map(
                            ([field, value]) => (
                              <div key={field}>
                                <p className="text-sm font-medium text-gray-500">
                                  {field}
                                </p>

                                <p className="break-words text-gray-800">
                                  {formatValue(value)}
                                </p>
                              </div>
                            )
                          )
                        )}
                      </div>
                    </div>

                    <div className="rounded-lg border border-purple-200 p-4">
                      <h3 className="font-semibold text-purple-700">
                        Incoming changes
                      </h3>

                      <p className="mt-1 break-all text-xs text-gray-500">
                        {conflict.incomingOperationId}
                      </p>

                      <div className="mt-4 space-y-3">
                        {Object.keys(incoming).length === 0 ? (
                          <p className="text-sm text-gray-500">
                            No payload fields.
                          </p>
                        ) : (
                          Object.entries(incoming).map(
                            ([field, value]) => (
                              <div key={field}>
                                <p className="text-sm font-medium text-gray-500">
                                  {field}
                                </p>

                                <p className="break-words text-gray-800">
                                  {formatValue(value)}
                                </p>
                              </div>
                            )
                          )
                        )}
                      </div>
                    </div>
                  </div>

                  {fields.length > 0 && (
                    <div className="mt-5 overflow-x-auto">
                      <h3 className="mb-3 font-semibold text-gray-700">
                        Change comparison
                      </h3>

                      <table className="w-full border-collapse text-left text-sm">
                        <thead>
                          <tr className="bg-gray-50">
                            <th className="border p-3">Field</th>
                            <th className="border p-3">Existing</th>
                            <th className="border p-3">Incoming</th>
                          </tr>
                        </thead>

                        <tbody>
                          {fields.map((field) => (
                            <tr key={field}>
                              <td className="border p-3 font-medium">
                                {field}
                              </td>

                              <td className="border p-3">
                                {formatValue(existing[field])}
                              </td>

                              <td className="border p-3">
                                {formatValue(incoming[field])}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  <div className="mt-6 flex flex-wrap gap-3">
                    <button
                      disabled={resolvingId !== null}
                      onClick={() =>
                        handleResolve(
                          conflict.id,
                          "KEEP_EXISTING"
                        )
                      }
                      className="rounded-lg bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {resolvingId === conflict.id
                        ? "Resolving..."
                        : "Keep Existing"}
                    </button>

                    <button
                      disabled={resolvingId !== null}
                      onClick={() =>
                        handleResolve(
                          conflict.id,
                          "KEEP_INCOMING"
                        )
                      }
                      className="rounded-lg bg-purple-600 px-5 py-2.5 font-medium text-white hover:bg-purple-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {resolvingId === conflict.id
                        ? "Resolving..."
                        : "Keep Incoming"}
                    </button>
                  </div>
                </section>
              );
            })}
          </div>
        )}

        <button
          onClick={() => void loadConflicts()}
          disabled={loading}
          className="mt-6 rounded-lg border border-gray-300 bg-white px-4 py-2 font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Refresh Conflicts
        </button>
      </div>
    </main>
  );
}

export default Conflicts;