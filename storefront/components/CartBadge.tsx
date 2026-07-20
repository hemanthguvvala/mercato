"use client";

import Link from "next/link";
import { useCart } from "@/lib/cart-context";

export default function CartBadge() {
  const { totalCount } = useCart();
  return (
    <Link href="/cart" className="hover:underline">
      Cart{totalCount > 0 ? ` (${totalCount})` : ""}
    </Link>
  );
}
