import { useState } from "react";
import { updateLocalRecord } from "../services/recordService";
import type { RecordData } from "../types/record";

interface EditRecordFormProps {
  record: RecordData;
  onUpdated: () => void;
  onCancel: () => void;
}

function EditRecordForm({
  record,
  onUpdated,
  onCancel,
}: EditRecordFormProps) {
  const [name, setName] = useState(record.name);
  const [phone, setPhone] = useState(record.phone);
  const [village, setVillage] = useState(record.village);
  const [address, setAddress] = useState(record.address);
  const [status, setStatus] = useState(record.status);

  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");
    setSaving(true);

    try {
      await updateLocalRecord(record.id, {
        name,
        phone,
        village,
        address,
        status,
      });

      onUpdated();
    } catch (error) {
      console.error("Failed to update local record:", error);
      setError("Failed to update record locally");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="mt-5 rounded-lg border bg-gray-50 p-4"
    >
      <h3 className="mb-4 text-lg font-semibold">
        Edit Record
      </h3>

      <div className="space-y-3">
        <input
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Name"
          required
          className="w-full rounded border px-3 py-2"
        />

        <input
          type="text"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          placeholder="Phone"
          required
          className="w-full rounded border px-3 py-2"
        />

        <input
          type="text"
          value={village}
          onChange={(event) => setVillage(event.target.value)}
          placeholder="Village"
          required
          className="w-full rounded border px-3 py-2"
        />

        <input
          type="text"
          value={address}
          onChange={(event) => setAddress(event.target.value)}
          placeholder="Address"
          required
          className="w-full rounded border px-3 py-2"
        />

        <select
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          className="w-full rounded border px-3 py-2"
        >
          <option value="ACTIVE">ACTIVE</option>
          <option value="INACTIVE">INACTIVE</option>
        </select>
      </div>

      {error && (
        <p className="mt-3 text-sm text-red-600">
          {error}
        </p>
      )}

      <div className="mt-4 flex gap-3">
        <button
          type="submit"
          disabled={saving}
          className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? "Saving..." : "Save"}
        </button>

        <button
          type="button"
          onClick={onCancel}
          disabled={saving}
          className="rounded bg-gray-500 px-4 py-2 text-white hover:bg-gray-600 disabled:opacity-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

export default EditRecordForm;