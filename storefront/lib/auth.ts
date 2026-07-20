import { cookies } from "next/headers";
import type { Session } from "@/lib/types";

/**
 * Server-only auth helpers (BFF model). The JWT lives in an httpOnly cookie the
 * browser's JS cannot read; only server code (Server Components, route handlers)
 * touches it here. Backend services still re-verify the token's signature via
 * JWKS on every call, so decoding-without-verifying here is safe — it's only used
 * to shape the UI, never as the security boundary.
 */

export const TOKEN_COOKIE = "mercato_token";

type JwtPayload = { sub?: string; roles?: string[]; exp?: number };

function decodeJwt(token: string): JwtPayload | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const json = Buffer.from(parts[1], "base64url").toString("utf8");
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/** Current session derived from the cookie, or null if absent/expired. */
export async function getSession(): Promise<Session | null> {
  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) return null;

  const payload = decodeJwt(token);
  if (!payload?.sub) return null;
  if (payload.exp && payload.exp * 1000 < Date.now()) return null; // expired

  return { username: payload.sub, roles: payload.roles ?? [] };
}

/** Raw bearer token for forwarding to authenticated backend calls (server-only). */
export async function getToken(): Promise<string | null> {
  return (await cookies()).get(TOKEN_COOKIE)?.value ?? null;
}
