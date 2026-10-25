import {
  AuthenticationError,
  defaultFirebaseTokenVerifier,
  TokenVerifier,
} from './auth';
import {
  AiGenerator,
  defaultGeminiGenerator,
  GeminiServiceError,
} from './gemini';
import { buildGenerationPrompt, buildTranslationPrompt } from './prompts';
import { QuotaManager } from './quota';
import { BackendEnv } from './types';
import {
  InputValidationError,
  MAX_REQUEST_BYTES,
  validateGenerateInput,
  validateTranslateInput,
} from './validation';

const CORS_HEADERS: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Request-Id',
  'Cache-Control': 'no-store',
};

// Lightweight in-isolate burst rate limiter (15 requests per 60s per UID) to protect free-tier quota
const uidBurstBuckets = new Map<string, { count: number; resetAt: number }>();

function checkUidBurstRateLimit(uid: string, nowMs: number): boolean {
  const windowMs = 60_000;
  const maxPerMinute = 15;
  const bucket = uidBurstBuckets.get(uid);
  if (!bucket || nowMs >= bucket.resetAt) {
    uidBurstBuckets.set(uid, { count: 1, resetAt: nowMs + windowMs });
    return true;
  }
  if (bucket.count >= maxPerMinute) {
    return false;
  }
  bucket.count += 1;
  return true;
}

function jsonResponse(status: number, payload: unknown): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...CORS_HEADERS,
    },
  });
}

async function parseBoundedJsonBody(request: Request): Promise<unknown> {
  const contentLength = Number(request.headers.get('content-length') ?? '0');
  if (contentLength > MAX_REQUEST_BYTES) {
    throw new InputValidationError('Request payload exceeds maximum allowed size (16 KB).');
  }
  const rawText = await request.text();
  if (rawText.length > MAX_REQUEST_BYTES) {
    throw new InputValidationError('Request payload exceeds maximum allowed size (16 KB).');
  }
  try {
    return JSON.parse(rawText);
  } catch {
    throw new InputValidationError('Malformed JSON request body.');
  }
}

export interface HandlerDependencies {
  tokenVerifier?: TokenVerifier;
  aiGenerator?: AiGenerator;
  nowProvider?: () => number;
}

export async function handleBackendRequest(
  request: Request,
  env: BackendEnv,
  deps: HandlerDependencies = {}
): Promise<Response> {
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  const url = new URL(request.url);
  // Normalize optional /api prefix so the exact same handler serves both Cloudflare Workers (/v1/*) and Express (/api/v1/*)
  const path = url.pathname.replace(/^\/api(?=\/v1\/)/, '');

  if (path === '/v1/health' && request.method === 'GET') {
    return jsonResponse(200, {
      ok: true,
      service: 'sn-creator-api',
      timezone: 'Asia/Karachi',
    });
  }

  const verifier = deps.tokenVerifier ?? defaultFirebaseTokenVerifier;
  const generator = deps.aiGenerator ?? defaultGeminiGenerator;
  const nowMs = deps.nowProvider ? deps.nowProvider() : Date.now();

  const firebaseProjectId = (env.FIREBASE_PROJECT_ID ?? '').trim();
  if (!firebaseProjectId) {
    return jsonResponse(500, {
      errorCode: 'SERVER_MISCONFIGURED',
      message: 'Backend FIREBASE_PROJECT_ID is not configured.',
    });
  }

  if (!env.QUOTA_DB) {
    return jsonResponse(500, {
      errorCode: 'SERVER_MISCONFIGURED',
      message: 'Backend quota database binding (QUOTA_DB) is not configured.',
    });
  }

  let uid: string;
  try {
    const verified = await verifier.verifyBearerHeader(
      request.headers.get('Authorization'),
      firebaseProjectId
    );
    uid = verified.uid;
  } catch (err) {
    if (err instanceof AuthenticationError) {
      return jsonResponse(401, {
        errorCode: 'AUTH_INVALID',
        message: err.message,
      });
    }
    return jsonResponse(500, {
      errorCode: 'SERVER_MISCONFIGURED',
      message: 'Authentication verification is misconfigured on the server.',
    });
  }

  const dailyLimit = Math.max(1, Number(env.DAILY_QUOTA_LIMIT ?? '10') || 10);
  const quotaManager = new QuotaManager(env.QUOTA_DB, dailyLimit);

  if (path === '/v1/quota' && request.method === 'GET') {
    const quota = await quotaManager.getQuotaStatus(uid, nowMs);
    return jsonResponse(200, { quota });
  }

  if (!checkUidBurstRateLimit(uid, nowMs)) {
    const quota = await quotaManager.getQuotaStatus(uid, nowMs);
    return jsonResponse(429, {
      errorCode: 'GEMINI_QUOTA_EXHAUSTED',
      message: 'Too many rapid requests. Please wait a few seconds and retry.',
      quota,
    });
  }

  if (path === '/v1/generate' && request.method === 'POST') {
    let input;
    try {
      const rawBody = await parseBoundedJsonBody(request);
      input = validateGenerateInput(rawBody);
    } catch (err) {
      if (err instanceof InputValidationError) {
        return jsonResponse(400, {
          errorCode: 'INVALID_INPUT',
          message: err.message,
        });
      }
      throw err;
    }

    const reservation = await quotaManager.reserveSlot(uid, input.requestId, nowMs);
    if (reservation.status === 'DUPLICATE') {
      return jsonResponse(409, {
        errorCode: 'DUPLICATE_REQUEST',
        message: 'This request is already being processed or was already completed.',
        quota: reservation.quota,
      });
    }
    if (reservation.status === 'EXHAUSTED') {
      return jsonResponse(429, {
        errorCode: 'DAILY_QUOTA_EXCEEDED',
        message: `You have reached all ${dailyLimit} daily generations. Quota resets at midnight Pakistan time (Asia/Karachi).`,
        quota: reservation.quota,
      });
    }

    try {
      const prompt = buildGenerationPrompt(input);
      const aiResult = await generator.generateText(
        prompt,
        env.GEMINI_API_KEY,
        env.GEMINI_MODEL
      );
      const updatedQuota = await quotaManager.commitReservation(
        uid,
        input.requestId,
        nowMs
      );

      // Generated content is returned directly to the authenticated user and NEVER stored on the server.
      return jsonResponse(200, {
        content: aiResult.text,
        tool: input.tool,
        game: input.game,
        language: input.language,
        duration: input.duration ?? null,
        model: aiResult.model,
        quota: updatedQuota,
      });
    } catch (err) {
      const restoredQuota = await quotaManager.rollbackReservation(
        uid,
        input.requestId,
        nowMs
      );
      if (err instanceof GeminiServiceError) {
        return jsonResponse(err.httpStatus, {
          errorCode: err.code,
          message: err.message,
          quota: restoredQuota,
        });
      }
      return jsonResponse(502, {
        errorCode: 'UPSTREAM_ERROR',
        message: 'AI generation failed. Your daily quota was restored.',
        quota: restoredQuota,
      });
    }
  }

  if (path === '/v1/translate' && request.method === 'POST') {
    let input;
    try {
      const rawBody = await parseBoundedJsonBody(request);
      input = validateTranslateInput(rawBody);
    } catch (err) {
      if (err instanceof InputValidationError) {
        return jsonResponse(400, {
          errorCode: 'INVALID_INPUT',
          message: err.message,
        });
      }
      throw err;
    }

    const reservation = await quotaManager.reserveSlot(uid, input.requestId, nowMs);
    if (reservation.status === 'DUPLICATE') {
      return jsonResponse(409, {
        errorCode: 'DUPLICATE_REQUEST',
        message: 'This translation request is already in progress or completed.',
        quota: reservation.quota,
      });
    }
    if (reservation.status === 'EXHAUSTED') {
      return jsonResponse(429, {
        errorCode: 'DAILY_QUOTA_EXCEEDED',
        message: `You have reached all ${dailyLimit} daily generations. Quota resets at midnight Pakistan time (Asia/Karachi).`,
        quota: reservation.quota,
      });
    }

    try {
      const prompt = buildTranslationPrompt(input);
      const aiResult = await generator.generateText(
        prompt,
        env.GEMINI_API_KEY,
        env.GEMINI_MODEL
      );
      const updatedQuota = await quotaManager.commitReservation(
        uid,
        input.requestId,
        nowMs
      );

      return jsonResponse(200, {
        content: aiResult.text,
        tool: input.tool,
        game: input.game,
        language: input.targetLanguage,
        duration: input.duration ?? null,
        model: aiResult.model,
        quota: updatedQuota,
      });
    } catch (err) {
      const restoredQuota = await quotaManager.rollbackReservation(
        uid,
        input.requestId,
        nowMs
      );
      if (err instanceof GeminiServiceError) {
        return jsonResponse(err.httpStatus, {
          errorCode: err.code,
          message: err.message,
          quota: restoredQuota,
        });
      }
      return jsonResponse(502, {
        errorCode: 'UPSTREAM_ERROR',
        message: 'Translation failed. Your daily quota was restored.',
        quota: restoredQuota,
      });
    }
  }

  return jsonResponse(404, {
    errorCode: 'NOT_FOUND',
    message: 'Requested API route does not exist.',
  });
}

export default {
  async fetch(request: Request, env: BackendEnv): Promise<Response> {
    return handleBackendRequest(request, env);
  },
};
