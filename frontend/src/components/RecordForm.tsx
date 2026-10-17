import { useRef, useState } from "react";
import { createLocalRecord } from "../services/recordService";

interface RecordFormProps {
  onRecordCreated: () => void;
}

function RecordForm({ onRecordCreated }: RecordFormProps) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [village, setVillage] = useState("");
  const [address, setAddress] = useState("");
  const [status, setStatus] = useState("ACTIVE");

  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
const submittingRef = useRef(false);

  async function handleSubmit(
  event: React.FormEvent<HTMLFormElement>
) {
  event.preventDefault();

  // Prevent duplicate submissions
  if (submittingRef.current) {
    return;
  }

  submittingRef.current = true;

  setError("");
  setSaving(true);

  try {
    await createLocalRecord({
      name,
      phone,
      village,
      address,
      status,
    });

    // Clear form
    setName("");
    setPhone("");
    setVillage("");
    setAddress("");
    setStatus("ACTIVE");

    // Tell Dashboard to reload local records
    onRecordCreated();

  } catch (error) {
    console.error("Failed to create local record:", error);
    setError("Failed to create record");
  } finally {
    submittingRef.current = false;
    setSaving(false);
  }
}

  return (
    <form
      onSubmit={handleSubmit}
      className="mb-8 rounded-lg bg-white p-6 shadow"
    >
      <h2 className="mb-5 text-xl font-semibold text-gray-800">
        Add Record
      </h2>

      <div className="grid gap-4 md:grid-cols-2">

        <input
          type="text"
          placeholder="Name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          required
          className="rounded border border-gray-300 px-4 py-2 outline-none focus:border-blue-500"
        />

        <input
          type="text"
          placeholder="Phone"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          required
          className="rounded border border-gray-300 px-4 py-2 outline-none focus:border-blue-500"
        />

        <input
          type="text"
          placeholder="Village"
          value={village}
          onChange={(event) => setVillage(event.target.value)}
          required
          className="rounded border border-gray-300 px-4 py-2 outline-none focus:border-blue-500"
        />

        <input
          type="text"
          placeholder="Address"
          value={address}
          onChange={(event) => setAddress(event.target.value)}
          className="rounded border border-gray-300 px-4 py-2 outline-none focus:border-blue-500"
        />

        <select
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          className="rounded border border-gray-300 px-4 py-2 outline-none focus:border-blue-500"
        >
          <option value="ACTIVE">ACTIVE</option>
          <option value="INACTIVE">INACTIVE</option>
        </select>

      </div>

      {error && (
        <p className="mt-4 text-red-600">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={saving}
        className="mt-5 rounded bg-blue-600 px-5 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {saving ? "Saving..." : "Add Record"}
      </button>
    </form>
  );
}

export default RecordForm;