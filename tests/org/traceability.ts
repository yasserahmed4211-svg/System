/**
 * Traceability helper — ORG-001 organization test suite
 * Every test MUST call trace() to log structured evidence.
 * Entries are printed to console as [TRACE] JSON lines.
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
