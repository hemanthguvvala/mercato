import { NextResponse } from "next/server";
import { API_BASE_URL } from "@/lib/config";
import { TOKEN_COOKIE } from "@/lib/auth";

/**
 * BFF login. The browser POSTs credentials to this same-origin route; the server
 * exchanges them with auth-service and stores the returned JWT in an httpOnly
 * cookie. The token never reaches client-side JavaScript.
 */
export async function POST(request: Request) {
  let body: { username?: string; password?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }

  const { username, password } = body;
  if (!username || !password) {
    return NextResponse.json(
      { error: "Username and password are required" },
      { status: 400 },
    );
  }

  const res = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
    cache: "no-store",
  });

  if (!res.ok) {
    // Collapse 401/403/400 from auth-service into a single opaque failure —
    // never leak which of username/password was wrong.
    return NextResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }

  const data: { token?: string } = await res.json();
  if (!data.token) {
    return NextResponse.json(
      { error: "Auth service returned no token" },
      { status: 502 },
    );
  }

  const response = NextResponse.json({ ok: true });
  response.cookies.set(TOKEN_COOKIE, data.token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60, // 1h; getSession also enforces the token's own exp
  });
  return response;
}
