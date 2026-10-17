import type {
  SyncOperationRequest,
  SyncOperationResponse,
} from "../types/sync";

const WS_URL = "ws://localhost:8080/ws/sync";

const MAX_RETRIES = 3;
const RESPONSE_TIMEOUT = 5000;
const RETRY_DELAY = 1000;

let socket: WebSocket | null = null;
let connectionPromise: Promise<void> | null = null;

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export function connectWebSocket(): Promise<void> {
  if (socket?.readyState === WebSocket.OPEN) {
    return Promise.resolve();
  }

  if (connectionPromise) {
    return connectionPromise;
  }

  connectionPromise = new Promise((resolve, reject) => {
    const ws = new WebSocket(WS_URL);

    socket = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
      connectionPromise = null;
      resolve();
    };

    ws.onerror = () => {
      connectionPromise = null;
      reject(new Error("WebSocket connection failed"));
    };

    ws.onclose = () => {
      console.log("WebSocket disconnected");
      socket = null;
      connectionPromise = null;
    };
  });

  return connectionPromise;
}

async function sendOnce(
  operation: SyncOperationRequest
): Promise<SyncOperationResponse> {
  await connectWebSocket();

  if (!socket || socket.readyState !== WebSocket.OPEN) {
    throw new Error("WebSocket is not connected");
  }

  const currentSocket = socket;

  return new Promise((resolve, reject) => {
    const timeout = window.setTimeout(() => {
      currentSocket.removeEventListener("message", handleMessage);
      reject(new Error("WebSocket response timeout"));
    }, RESPONSE_TIMEOUT);

    function handleMessage(event: MessageEvent) {
      window.clearTimeout(timeout);
      currentSocket.removeEventListener("message", handleMessage);

      try {
        const response: SyncOperationResponse = JSON.parse(
          event.data
        );

        resolve(response);
      } catch {
        reject(new Error("Invalid WebSocket response"));
      }
    }

    currentSocket.addEventListener("message", handleMessage);

    currentSocket.send(JSON.stringify(operation));
  });
}

export async function sendWebSocketOperation(
  operation: SyncOperationRequest
): Promise<SyncOperationResponse> {
  let lastError: unknown;

  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      return await sendOnce(operation);
    } catch (error) {
      lastError = error;

      console.error(
        `WebSocket sync attempt ${attempt} failed:`,
        error
      );

      if (socket) {
        socket.close();
        socket = null;
      }

      connectionPromise = null;

      if (attempt < MAX_RETRIES) {
        await wait(RETRY_DELAY);
      }
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error("WebSocket synchronization failed");
}

export function disconnectWebSocket(): void {
  if (socket) {
    socket.close();
    socket = null;
  }

  connectionPromise = null;
}