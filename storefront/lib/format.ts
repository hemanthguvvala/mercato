// Presentation helpers. Kept tiny and dependency-free.

const inr = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

/** Format a rupee amount for display, e.g. 1299 -> "₹1,299". */
export function formatPrice(value: number): string {
  return inr.format(value);
}
