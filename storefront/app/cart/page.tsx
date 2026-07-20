"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCart } from "@/lib/cart-context";
import { formatPrice } from "@/lib/format";
import type { Order } from "@/lib/types";

export default function CartPage() {
  const { items, setQuantity, removeItem, clear, totalPrice, totalCount } =
    useCart();
  const router = useRouter();
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState("");
  const [order, setOrder] = useState<Order | null>(null);

  async function checkout() {
    setError("");
    setPlacing(true);
    try {
      const res = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          lines: items.map((i) => ({
            productId: i.productId,
            quantity: i.quantity,
          })),
        }),
      });
      if (res.status === 401) {
        router.push("/login");
        return;
      }
      if (!res.ok) {
        setError("Could not place the order. Please try again.");
        return;
      }
      const placed: Order = await res.json();
      setOrder(placed);
      clear();
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setPlacing(false);
    }
  }

  // Confirmation view after a successful checkout.
  if (order) {
    return (
      <div className="mx-auto max-w-2xl p-8">
        <h1 className="text-2xl font-semibold">Order placed ✓</h1>
        <p className="mt-2 text-zinc-600 dark:text-zinc-400">
          Order #{order.id} — thanks, {order.customerName}.
        </p>
        <ul className="mt-6 divide-y divide-black/10 dark:divide-white/15">
          {order.items.map((it) => (
            <li key={it.productId} className="flex justify-between py-3">
              <span>
                {it.productName} × {it.quantity}
              </span>
              <span>{formatPrice(it.unitPrice * it.quantity)}</span>
            </li>
          ))}
        </ul>
        <Link
          href="/products"
          className="mt-6 inline-block hover:underline"
        >
          Continue shopping →
        </Link>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-2xl p-8">
        <h1 className="text-2xl font-semibold">Your cart</h1>
        <p className="mt-2 text-zinc-500">Your cart is empty.</p>
        <Link href="/products" className="mt-4 inline-block hover:underline">
          Browse products →
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl p-8">
      <h1 className="mb-6 text-2xl font-semibold">Your cart</h1>
      <ul className="divide-y divide-black/10 dark:divide-white/15">
        {items.map((i) => (
          <li
            key={i.productId}
            className="flex items-center justify-between gap-4 py-4"
          >
            <div>
              <p className="font-medium">{i.name}</p>
              <p className="text-sm text-zinc-500">{formatPrice(i.price)} each</p>
            </div>
            <div className="flex items-center gap-3">
              <input
                type="number"
                min={1}
                value={i.quantity}
                onChange={(e) => setQuantity(i.productId, Number(e.target.value))}
                className="w-16 rounded-md border border-black/15 px-2 py-1 dark:border-white/20 dark:bg-transparent"
              />
              <span className="w-24 text-right font-medium">
                {formatPrice(i.price * i.quantity)}
              </span>
              <button
                onClick={() => removeItem(i.productId)}
                className="text-sm text-red-600 hover:underline"
              >
                Remove
              </button>
            </div>
          </li>
        ))}
      </ul>
      <div className="mt-6 flex items-center justify-between">
        <span className="text-lg font-semibold">Total ({totalCount})</span>
        <span className="text-lg font-semibold">{formatPrice(totalPrice)}</span>
      </div>
      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}
      <button
        onClick={checkout}
        disabled={placing}
        className="mt-6 w-full rounded-md bg-black px-4 py-3 text-sm font-medium text-white transition-colors hover:bg-zinc-800 disabled:opacity-50 dark:bg-white dark:text-black dark:hover:bg-zinc-200"
      >
        {placing ? "Placing order…" : "Checkout"}
      </button>
    </div>
  );
}
