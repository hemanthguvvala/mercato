import { getProducts } from "@/lib/api/products";
import ProductCard from "@/components/ProductCard";

export const metadata = { title: "Products — Mercato" };

export default async function ProductsPage() {
  const products = await getProducts();

  return (
    <div className="mx-auto max-w-5xl p-8">
      <h1 className="mb-6 text-2xl font-semibold">Products</h1>
      {products.length === 0 ? (
        <p className="text-zinc-500">No products yet.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  );
}
