/**
 * Traceability helper — masterdata test suite
 * Every test MUST call trace() to log structured evidence.
 */

export interface TraceLog {
  tcId: string;
  endpoint: string;
  requestPayload: unknown;
  response: unknown;
  statusCode: number;
  timestamp: string;
}

export function trace(log: Omit<TraceLog, 'timestamp'>): void {
  const entry: TraceLog = { ...log, timestamp: new Date().toISOString() };
  console.log('[TRACE]', JSON.stringify(entry));
}
