import { useEffect } from "react";
import { syncPendingOperations } from "../services/syncService";

export function useNetworkSync(
  onSynced?: () => void | Promise<void>
) {
  useEffect(() => {
    async function handleOnline() {
      console.log(
        "Network is back online. Starting synchronization..."
      );

      await syncPendingOperations();

      if (onSynced) {
        await onSynced();
      }
    }

    window.addEventListener("online", handleOnline);

    return () => {
      window.removeEventListener("online", handleOnline);
    };
  }, [onSynced]);
}