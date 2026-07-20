/**
 * Central storefront config — one source of truth for the API gateway URL, so
 * pages and data functions never re-read process.env inline.
 *
 * All backend calls happen server-side (Server Components + route-handler BFFs),
 * so this is only ever read on the server. In a real deploy you'd point the server
 * at an internal gateway address distinct from any public one.
 */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8090";
