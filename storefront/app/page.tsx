import Link from "next/link";

export default function Home() {
  return (
    <div className="mx-auto max-w-3xl p-8">
      <h1 className="text-3xl font-bold tracking-tight">Welcome to Mercato</h1>
      <p className="mt-3 max-w-prose text-zinc-600 dark:text-zinc-400">
        A small distributed storefront — a React front end over a Spring Boot
        microservices backend: an API gateway, catalog, orders, payments, and a
        saga-driven checkout.
      </p>
      <Link
        href="/products"
        className="mt-6 inline-block rounded-md bg-black px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-black dark:hover:bg-zinc-200"
      >
        Browse products
      </Link>
    </div>
  );
}
