import Link from "next/link";
import type { Product } from "@/lib/types";
import { formatPrice } from "@/lib/format";

/**
 * Presentational product tile. Links to the detail route; the add-to-cart action
 * is layered in once the cart context exists.
 */
export default function ProductCard({ product }: { product: Product }) {
  return (
    <Link
      href={`/products/${product.id}`}
      className="flex flex-col rounded-lg border border-black/10 p-5 transition-shadow hover:shadow-md dark:border-white/15"
    >
      <h3 className="text-base font-medium">{product.name}</h3>
      <p className="mt-2 text-lg font-semibold">{formatPrice(product.price)}</p>
    </Link>
  );
}
