// Domain types shared across the storefront. These mirror the backend DTOs
// (catalog Product, order OrderResponse) so the UI and the API layer agree on shape.

export type Product = {
  id: number;
  name: string;
  price: number;
  version: number;
};

export type OrderItem = {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
};

export type Order = {
  id: number;
  customerName: string;
  items: OrderItem[];
};

// A line in the client-side cart. There is no cart service — the cart lives in
// the browser and is only materialized into an order at checkout.
export type CartItem = {
  productId: number;
  name: string;
  price: number;
  quantity: number;
};

// The authenticated user, derived from the JWT held in an httpOnly cookie.
export type Session = {
  username: string;
  roles: string[];
};
