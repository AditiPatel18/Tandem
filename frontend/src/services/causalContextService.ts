const CAUSAL_CONTEXT_KEY = "tandem_causal_context";

export type CausalContext = Record<string, number>;

export function getCausalContext(): CausalContext {
  const storedContext =
    localStorage.getItem(CAUSAL_CONTEXT_KEY);

  if (!storedContext) {
    return {};
  }

  try {
    const parsedContext = JSON.parse(
      storedContext
    ) as CausalContext;

    return parsedContext;
  } catch {
    return {};
  }
}

export function updateCausalContext(
  deviceId: string,
  sequenceNumber: number
): void {
  const currentContext =
    getCausalContext();

  const currentSequence =
    currentContext[deviceId] ?? 0;

  if (sequenceNumber > currentSequence) {
    currentContext[deviceId] =
      sequenceNumber;

    localStorage.setItem(
      CAUSAL_CONTEXT_KEY,
      JSON.stringify(currentContext)
    );
  }
}

export function clearCausalContext(): void {
  localStorage.removeItem(
    CAUSAL_CONTEXT_KEY
  );
}