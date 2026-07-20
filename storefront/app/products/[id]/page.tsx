import Link from "next/link";
import { notFound } from "next/navigation";
import { getProduct } from "@/lib/api/products";
import { formatPrice } from "@/lib/format";
import AddToCartButton from "@/components/AddToCartButton";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const productId = Number(id);
  if (!Number.isInteger(productId)) notFound();

  const product = await getProduct(productId);
  if (!product) notFound();

  return (
    <div className="mx-auto max-w-2xl p-8">
      <Link href="/products" className="text-sm text-zinc-500 hover:underline">
        ← Back to products
      </Link>
      <h1 className="mt-4 text-2xl font-semibold">{product.name}</h1>
      <p className="mt-3 text-xl font-bold">{formatPrice(product.price)}</p>
      <div className="mt-6">
        <AddToCartButton product={product} />
      </div>
    </div>
  );
}
