/**
 * DB Validation Helper — API-Proxy layer
 *
 * NOTE: Oracle MCP direct connection is unavailable (port 1892 not reachable).
 * DB state is validated INDIRECTLY via the backend REST API (read-back approach).
 * Each validate call issues a real HTTP GET to the backend and inspects the response.
 */

import type { APIRequestContext } from '@playwright/test';

export interface DbValidateResult {
  validated: boolean;
  evidence: Record<string, unknown>;
}

/**
 * Validate that a record was inserted by fetching it via GET.
 */
export async function validateInsert(
  ctx: APIRequestContext,
  endpoint: string,
  id: number | string,
  headers: Record<string, string>,
): Promise<DbValidateResult> {
  const resp = await ctx.get(`${endpoint}/${id}`, { headers });
  const body = await resp.json().catch(() => ({}));
  return {
    validated: resp.status() === 200 && !!body?.data,
    evidence: { httpStatus: resp.status(), data: body?.data ?? null },
  };
}

/**
 * Validate that specific fields match expected values after an update.
 */
export async function validateUpdate(
  ctx: APIRequestContext,
  endpoint: string,
  id: number | string,
  expectedFields: Record<string, unknown>,
  headers: Record<string, string>,
): Promise<DbValidateResult> {
  const resp = await ctx.get(`${endpoint}/${id}`, { headers });
  const body = await resp.json().catch(() => ({}));
  const data = body?.data ?? {};
  const allMatch = Object.entries(expectedFields).every(([k, v]) => data[k] === v);
  return {
    validated: resp.status() === 200 && allMatch,
    evidence: {
      httpStatus: resp.status(),
      expected: expectedFields,
      actual: Object.fromEntries(Object.keys(expectedFields).map((k) => [k, data[k]])),
    },
  };
}

/**
 * Validate usage info for a master lookup (canDelete, canDeactivate, totalDetails).
 */
export async function validateUsage(
  ctx: APIRequestContext,
  endpoint: string,
  id: number | string,
  headers: Record<string, string>,
): Promise<{ usageData: Record<string, unknown> }> {
  const resp = await ctx.get(`${endpoint}/${id}/usage`, { headers });
  const body = await resp.json().catch(() => ({}));
  return { usageData: body?.data ?? {} };
}
