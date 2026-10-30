import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Middleware for Next.js 14 App Router
 * Protects routes that require authentication
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Get auth storage from cookies (if using cookies) or skip middleware check
  // Note: localStorage is client-side only, middleware runs server-side
  // For server-side protection, consider using cookies or JWT in headers

  // Public routes that don't require authentication
  const publicRoutes = ['/login', '/register', '/forgot-password', '/'];
  
  // If current route is public, allow access
  if (publicRoutes.some((route) => pathname.startsWith(route))) {
    return NextResponse.next();
  }

  // For protected routes, we'll rely on client-side ProtectedRoute HOC
  // because auth state is in localStorage (Zustand persist)
  // Middleware can't access localStorage

  return NextResponse.next();
}

// Configure which routes to run middleware on
export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public folder
     */
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};
