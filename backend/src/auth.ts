import { createRemoteJWKSet, jwtVerify } from 'jose';
import { VerifiedFirebaseIdentity } from './types';

const FIREBASE_JWKS_URL = new URL(
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'
);

// Cached remote JWK set handled via standard Web Crypto API (compatible with Cloudflare Workers & Node 22)
const firebaseJwks = createRemoteJWKSet(FIREBASE_JWKS_URL);

export class AuthenticationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthenticationError';
  }
}

export interface TokenVerifier {
  verifyBearerHeader(
    authorizationHeader: string | null | undefined,
    firebaseProjectId: string
  ): Promise<VerifiedFirebaseIdentity>;
}

export const defaultFirebaseTokenVerifier: TokenVerifier = {
  async verifyBearerHeader(
    authorizationHeader: string | null | undefined,
    firebaseProjectId: string
  ): Promise<VerifiedFirebaseIdentity> {
    if (!authorizationHeader || !authorizationHeader.startsWith('Bearer ')) {
      throw new AuthenticationError('Missing Authorization Bearer token.');
    }

    const token = authorizationHeader.slice('Bearer '.length).trim();
    if (!token || token.length > 8192) {
      throw new AuthenticationError('Invalid authentication token.');
    }

    const cleanProjectId = firebaseProjectId.trim();
    if (!cleanProjectId) {
      throw new Error('SERVER_MISCONFIGURED: FIREBASE_PROJECT_ID is not configured.');
    }

    try {
      const { payload } = await jwtVerify(token, firebaseJwks, {
        issuer: `https://securetoken.google.com/${cleanProjectId}`,
        audience: cleanProjectId,
        algorithms: ['RS256'],
      });

      const uid = typeof payload.sub === 'string' ? payload.sub.trim() : '';
      if (!uid || uid.length > 128) {
        throw new AuthenticationError('Token subject (UID) is missing or invalid.');
      }

      return { uid };
    } catch (err) {
      if (err instanceof AuthenticationError) throw err;
      throw new AuthenticationError('Invalid or expired Firebase authentication token.');
    }
  },
};
