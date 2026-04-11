import { readAddressToken } from '../utils/token-bridge';

function resolveStorage(storage) {
  if (storage) {
    return storage;
  }
  if (typeof localStorage !== 'undefined') {
    return localStorage;
  }
  return null;
}

function isPublicRoute(route) {
  return Array.isArray(route?.matched)
    && route.matched.some(record => Boolean(record?.meta?.public));
}

export function resolveAuthRedirect(to, storage) {
  const token = readAddressToken(resolveStorage(storage));
  if (token && to?.path === '/login') {
    return {
      path: '/standard/list'
    };
  }
  if (isPublicRoute(to)) {
    return null;
  }
  if (token) {
    return null;
  }
  return {
    path: '/login',
    query: {
      redirect: to?.fullPath || to?.path || '/standard/list'
    }
  };
}

export function registerAuthGuard(router, storage) {
  if (!router || typeof router.beforeEach !== 'function') {
    return;
  }
  router.beforeEach((to, from, next) => {
    const redirect = resolveAuthRedirect(to, storage);
    next(redirect || undefined);
  });
}
