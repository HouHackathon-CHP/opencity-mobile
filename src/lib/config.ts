export const IDENTITY_URL = (process.env.EXPO_PUBLIC_IDENTITY_URL || 'http://localhost:8081').replace(/\/$/, '');
export const GATEWAY_URL = (process.env.EXPO_PUBLIC_GATEWAY_URL || 'http://localhost:8000').replace(/\/$/, '');
export const MAP_STYLE = process.env.EXPO_PUBLIC_MAP_STYLE_URL || {
  version: 8 as const,
  sources: {
    openstreetmap: {
      type: 'raster' as const,
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '© OpenStreetMap contributors',
    },
  },
  layers: [{ id: 'openstreetmap', type: 'raster' as const, source: 'openstreetmap' }],
};
export const REDIRECT_URI = 'opencity://auth';
export const CLIENT_ID = 'dxos-mobile';
export const DISCOVERY = {
  authorizationEndpoint: `${IDENTITY_URL}/realms/dxos/protocol/openid-connect/auth`,
  tokenEndpoint: `${IDENTITY_URL}/realms/dxos/protocol/openid-connect/token`,
  revocationEndpoint: `${IDENTITY_URL}/realms/dxos/protocol/openid-connect/revoke`,
};
