import { NextResponse } from "next/server";
import { API_BASE_URL } from "@/lib/config";
import { getSession, getToken } from "@/lib/auth";

type Line = { productId: number; quantity: number };

/**
 * BFF checkout. Reads the session server-side, so the customer identity comes
 * from the verified token — never from client input. Injects the bearer token
 * and a fresh Idempotency-Key (required by order-service).
 *
 * Note: the key is generated per request here; a fully hardened flow would have
 * the client supply a stable key so network-level retries of the same checkout
 * dedupe. The client button-disable guards the common double-click case.
 */
export async function POST(request: Request) {
  const session = await getSession();
  const token = await getToken();
  if (!session || !token) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
  }

  let body: { lines?: Line[] };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }

  const lines = body.lines ?? [];
  if (lines.length === 0) {
    return NextResponse.json({ error: "Cart is empty" }, { status: 400 });
  }

  const res = await fetch(`${API_BASE_URL}/orders`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "Idempotency-Key": crypto.randomUUID(),
    },
    body: JSON.stringify({ customerName: session.username, lines }),
    cache: "no-store",
  });

  if (!res.ok) {
    return NextResponse.json(
      { error: "Order creation failed" },
      { status: res.status },
    );
  }

  const order = await res.json();
  return NextResponse.json(order, { status: 201 });
}
