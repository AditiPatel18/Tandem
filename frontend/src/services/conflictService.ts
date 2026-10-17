const API_BASE_URL = "http://localhost:8080/api";

export interface Conflict {
  id: number;
  recordId: number;
  existingOperationId: string;
  incomingOperationId: string;
  existingPayload: string;
  incomingPayload: string;
  status: string;
  createdAt: string;
}

export type ConflictResolution =
  | "KEEP_EXISTING"
  | "KEEP_INCOMING";

export async function getOpenConflicts(): Promise<Conflict[]> {
  const response = await fetch(
    `${API_BASE_URL}/conflicts/open`
  );

  if (!response.ok) {
    throw new Error("Failed to load conflicts");
  }

  return response.json();
}

export async function resolveConflict(
  conflictId: number,
  resolution: ConflictResolution
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/conflicts/${conflictId}/resolve`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ resolution }),
    }
  );

  if (!response.ok) {
    const error = await response.json().catch(() => null);

    throw new Error(
      error?.message ?? "Failed to resolve conflict"
    );
  }
}