type Product = {
    id: number;
    name: string;
    price: number;
    version: number;
};

export default async function ProductsPage(){
    const res = await fetch("http://localhost:8090/products", {
        cache: "no-store",
    });

    if(!res.ok){
        throw new Error(`Catalog request failed: ${res.status}`);
    }

    const products:Product[] = await res.json();

    return (
        <main className="mx-auto max-w-3x1 p-8">
            <h1 className="mb-6 text-2x1 font-semibold">Products</h1>
            <ul className="space-y-2">
                {products.map( (product) => (
                    <li key= {product.id} className="flex justify-between border-b py-2">
                        <span>{product.name}</span>
                        <span>₹{product.price}</span>
                    </li>
                ) )}
            </ul>
        </main>
    )
}