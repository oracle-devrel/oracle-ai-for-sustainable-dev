const trimTrailingSlash = (value) => (value || '').replace(/\/+$/, '');

const ensureLeadingSlash = (value) => {
  if (!value || value === '/') {
    return '';
  }
  const trimmed = trimTrailingSlash(value);
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
};

export const APP_BASE_PATH = ensureLeadingSlash(
  process.env.REACT_APP_BASE_PATH || process.env.PUBLIC_URL || ''
);

export const API_BASE_PATH = ensureLeadingSlash(
  process.env.REACT_APP_API_BASE_PATH || `${APP_BASE_PATH}/api`
);

export const ACCOUNT_SERVICE_URL = trimTrailingSlash(
  process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL || `${APP_BASE_PATH}/accounts-api`
);

export const TRANSFER_SERVICE_URL = trimTrailingSlash(
  process.env.REACT_APP_MICROTX_TRANSFER_SERVICE_URL || `${APP_BASE_PATH}/transfer-api`
);

export const KAFKA_SERVICE_URL = trimTrailingSlash(
  process.env.REACT_APP_KAFKA_TXEVENTQ_SERVICE_URL || `${API_BASE_PATH}/kafka`
);

export const TRUECACHE_SERVICE_URL = trimTrailingSlash(
  process.env.REACT_APP_TRUECACHE_STOCK_SERVICE_URL || `${API_BASE_PATH}/truecache`
);

export const GRAFANA_URL = trimTrailingSlash(
  process.env.REACT_APP_GRAFANA_URL || `${APP_BASE_PATH}/grafana`
);

export const joinUrl = (base, path = '') => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${trimTrailingSlash(base)}${normalizedPath}`;
};

export const assetPath = (path) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${APP_BASE_PATH}${normalizedPath}`;
};
