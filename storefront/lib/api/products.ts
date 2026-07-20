import { API_BASE_URL } from "@/lib/config";
import type { Product } from "@/lib/types";

/**
 * Catalog data-access layer. Pages call these instead of fetching inline, so
 * routing stays thin and the data logic is reusable and testable in one place.
 * Runs server-side; GET /products is public so no token is required.
 */

export async function getProducts(): Promise<Product[]> {
  const res = await fetch(`${API_BASE_URL}/products`, { cache: "no-store" });
  if (!res.ok) {
    throw new Error(`Failed to load products (${res.status})`);
  }
  return res.json();
}

export async function getProduct(id: number): Promise<Product | null> {
  const res = await fetch(`${API_BASE_URL}/products/${id}`, {
    cache: "no-store",
  });
  if (res.status === 404) return null;
  if (!res.ok) {
    throw new Error(`Failed to load product ${id} (${res.status})`);
  }
  return res.json();
}
