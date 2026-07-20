import Link from "next/link";
import type { Session } from "@/lib/types";
import LogoutButton from "@/components/LogoutButton";
import CartBadge from "@/components/CartBadge";

/**
 * Top navigation, rendered by the root layout so it appears on every route.
 * Session is resolved server-side and passed in; the cart badge and logout are
 * small client islands.
 */
export default function Nav({ session }: { session: Session | null }) {
  return (
    <header className="flex items-center justify-between border-b border-black/10 px-8 py-4 dark:border-white/15">
      <Link href="/" className="text-xl font-bold tracking-tight">
        Mercato
      </Link>
      <nav className="flex items-center gap-6 text-sm">
        <Link href="/products" className="hover:underline">
          Products
        </Link>
        <CartBadge />
        {session ? (
          <>
            <span className="text-zinc-500">Hi, {session.username}</span>
            <LogoutButton />
          </>
        ) : (
          <Link href="/login" className="hover:underline">
            Login
          </Link>
        )}
      </nav>
    </header>
  );
}
