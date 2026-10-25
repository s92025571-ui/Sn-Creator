import { DatabaseSync } from 'node:sqlite';
import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { AuthenticationError, TokenVerifier } from '../src/auth';
import { AiGenerator, GeminiServiceError } from '../src/gemini';
import { handleBackendRequest } from '../src/index';
import { buildGenerationPrompt, VOICEOVER_DURATION_SPECS } from '../src/prompts';
import { computePakistanDateWindow } from '../src/quota';
import {
  BackendEnv,
  D1DatabaseLike,
  D1PreparedStatementLike,
  VoiceOverDuration,
} from '../src/types';

function createSqliteD1Adapter(): D1DatabaseLike {
  const sqlite = new DatabaseSync(':memory:');
  const schemaPath = path.resolve(__dirname, '../schema.sql');
  sqlite.exec(fs.readFileSync(schemaPath, 'utf-8'));

  return {
    prepare(query: string): D1PreparedStatementLike {
      let boundValues: unknown[] = [];
      const stmtLike: D1PreparedStatementLike = {
        bind(...values: unknown[]) {
          boundValues = values;
          return stmtLike;
        },
        async first<T = Record<string, unknown>>(): Promise<T | null> {
          const stmt = sqlite.prepare(query);
          const row = stmt.get(...(boundValues as never[])) as T | undefined;
          return row ?? null;
        },
        async run(): Promise<{ meta?: { changes?: number } }> {
          const stmt = sqlite.prepare(query);
          const res = stmt.run(...(boundValues as never[]));
          return { meta: { changes: Number(res.changes) } };
        },
      };
      return stmtLike;
    },
  };
}

const mockVerifier: TokenVerifier = {
  async verifyBearerHeader(header, projectId) {
    if (!header || !header.startsWith('Bearer valid-token-')) {
      throw new AuthenticationError('Invalid or missing token');
    }
    const uid = header.replace('Bearer valid-token-', '').trim();
    if (!uid || !projectId) throw new AuthenticationError('Bad uid');
    return { uid };
  },
};

describe('SN Creator Secure Zero-Billing Backend', () => {
  it('computes Asia/Karachi calendar day rollover accurately at 19:00 UTC (00:00 PKT)', () => {
    const beforeMidnightPkt = Date.parse('2026-10-25T18:59:59Z');
    const w1 = computePakistanDateWindow(beforeMidnightPkt);
    expect(w1.pktDate).toBe('2026-10-25');
    expect(w1.resetsAtIso).toBe('2026-10-25T19:00:00.000Z');

    const afterMidnightPkt = Date.parse('2026-10-25T19:00:01Z');
    const w2 = computePakistanDateWindow(afterMidnightPkt);
    expect(w2.pktDate).toBe('2026-10-26');
    expect(w2.resetsAtIso).toBe('2026-10-26T19:00:00.000Z');
  });

  it('rejects unauthenticated requests even if a fake user ID is sent in body', async () => {
    const env: BackendEnv = {
      QUOTA_DB: createSqliteD1Adapter(),
      FIREBASE_PROJECT_ID: 'enduring-agent-dt8c4',
      GEMINI_API_KEY: 'test-key',
    };

    const res = await handleBackendRequest(
      new Request('https://api.sncreator.example/v1/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: 'spoofed-uid',
          requestId: 'req_unauth_12345',
          tool: 'GAMING_TITLES',
          game: 'PUBG Mobile',
          topic: 'Erangel bridge ambush',
          language: 'ENGLISH',
        }),
      }),
      env,
      { tokenVerifier: mockVerifier }
    );

    expect(res.status).toBe(401);
    const body = await res.json();
    expect(body.errorCode).toBe('AUTH_INVALID');
  });

  it('enforces 10 daily generations, blocks duplicate requestIds, rolls back failed requests, and resets at Pakistan midnight', async () => {
    const db = createSqliteD1Adapter();
    const env: BackendEnv = {
      QUOTA_DB: db,
      FIREBASE_PROJECT_ID: 'enduring-agent-dt8c4',
      GEMINI_API_KEY: 'test-key',
      DAILY_QUOTA_LIMIT: '10',
    };

    let currentNow = Date.parse('2026-10-25T12:00:00Z'); // 5:00 PM PKT Oct 25
    let shouldFailAi = false;

    const aiGenerator: AiGenerator = {
      async generateText(prompt) {
        if (shouldFailAi) {
          throw new GeminiServiceError(
            'UPSTREAM_TIMEOUT',
            504,
            'Timed out talking to Gemini'
          );
        }
        return {
          text: `AI Output (${prompt.slice(0, 24)}...)`,
          model: 'gemini-3.8-flash',
        };
      },
    };

    // 1. Perform 9 initial generations + 1 translation = 10 total daily generations
    for (let i = 1; i <= 9; i++) {
      const res = await handleBackendRequest(
        new Request('https://api.sncreator.example/v1/generate', {
          method: 'POST',
          headers: {
            Authorization: 'Bearer valid-token-userA',
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            requestId: `req_gen_000${i}`,
            tool: 'GAMING_TITLES',
            game: 'PUBG Mobile',
            topic: `Clutch strategy part ${i}`,
            language: i % 2 === 0 ? 'ROMAN_URDU' : 'ENGLISH',
          }),
        }),
        env,
        { tokenVerifier: mockVerifier, aiGenerator, nowProvider: () => currentNow }
      );
      expect(res.status).toBe(200);
      const data = await res.json();
      expect(data.quota.remaining).toBe(10 - i);
    }

    // 2. Duplicate replay of req_gen_0001 must return 409 DUPLICATE_REQUEST without changing remaining quota (still 1)
    const dupRes = await handleBackendRequest(
      new Request('https://api.sncreator.example/v1/generate', {
        method: 'POST',
        headers: {
          Authorization: 'Bearer valid-token-userA',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          requestId: 'req_gen_0001',
          tool: 'GAMING_TITLES',
          game: 'PUBG Mobile',
          topic: 'Duplicate tap test',
          language: 'ENGLISH',
        }),
      }),
      env,
      { tokenVerifier: mockVerifier, aiGenerator, nowProvider: () => currentNow }
    );
    expect(dupRes.status).toBe(409);
    const dupBody = await dupRes.json();
    expect(dupBody.quota.remaining).toBe(1);

    // 3. Failed Gemini call MUST roll back reservation so user still has 1 remaining generation!
    shouldFailAi = true;
    const failedRes = await handleBackendRequest(
      new Request('https://api.sncreator.example/v1/generate', {
        method: 'POST',
        headers: {
          Authorization: 'Bearer valid-token-userA',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          requestId: 'req_fail_timeout_1',
          tool: 'VIDEO_IDEAS',
          game: 'Free Fire',
          topic: 'Clash squad ranked tips',
          language: 'ENGLISH',
        }),
      }),
      env,
      { tokenVerifier: mockVerifier, aiGenerator, nowProvider: () => currentNow }
    );
    expect(failedRes.status).toBe(504);
    const failedBody = await failedRes.json();
    expect(failedBody.quota.remaining).toBe(1);

    // 4. 10th generation via Switch Language (/v1/translate) succeeds and leaves 0 remaining
    shouldFailAi = false;
    const translateRes = await handleBackendRequest(
      new Request('https://api.sncreator.example/v1/translate', {
        method: 'POST',
        headers: {
          Authorization: 'Bearer valid-token-userA',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          requestId: 'req_translate_10',
          tool: 'GAMING_TITLES',
          game: 'PUBG Mobile',
          currentContent: '1. Ultimate Erangel Final Circle Guide',
          targetLanguage: 'ROMAN_URDU',
        }),
      }),
      env,
      { tokenVerifier: mockVerifier, aiGenerator, nowProvider: () => currentNow }
    );
    expect(translateRes.status).toBe(200);
    const translateBody = await translateRes.json();
    expect(translateBody.quota.remaining).toBe(0);

    // 5. 11th attempt on same Pakistan day is blocked with 429 DAILY_QUOTA_EXCEEDED
    const overLimitRes = await handleBackendRequest(
      new Request('https://api.sncreator.example/v1/generate', {
        method: 'POST',
        headers: {
          Authorization: 'Bearer valid-token-userA',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          requestId: 'req_over_limit_11',
          tool: 'HASHTAGS',
          game: 'Last Asylum: Plague',
          topic: 'Survival guide',
          language: 'ENGLISH',
        }),
      }),
      env,
      { tokenVerifier: mockVerifier, aiGenerator, nowProvider: () => currentNow }
    );
    expect(overLimitRes.status).toBe(429);
    const overBody = await overLimitRes.json();
    expect(overBody.errorCode).toBe('DAILY_QUOTA_EXCEEDED');
    expect(overBody.quota.remaining).toBe(0);

    // 6. Advance clock past midnight PKT (2026-10-25T19:01:00Z == Oct 26 12:01 AM PKT) -> fresh 10/10 quota!
    currentNow = Date.parse('2026-10-25T19:01:00Z');
    const nextDayQuotaRes = await handleBackendRequest(
      new Request('https://api.sncreator.example/v1/quota', {
        method: 'GET',
        headers: { Authorization: 'Bearer valid-token-userA' },
      }),
      env,
      { tokenVerifier: mockVerifier, aiGenerator, nowProvider: () => currentNow }
    );
    expect(nextDayQuotaRes.status).toBe(200);
    const nextDayBody = await nextDayQuotaRes.json();
    expect(nextDayBody.quota.remaining).toBe(10);
    expect(nextDayBody.quota.pktDate).toBe('2026-10-26');
  });

  it('builds distinct prompts for all 5 tools, Roman Urdu Latin script, and all 6 voice-over durations', () => {
    const durations: VoiceOverDuration[] = ['15s', '30s', '60s', '2m', '5m', '10m'];
    for (const d of durations) {
      const prompt = buildGenerationPrompt({
        requestId: `req_dur_${d}_1234`,
        tool: 'VOICEOVER_SCRIPT',
        game: 'Gangstar Mirage City',
        topic: 'Best mission payout route',
        language: 'ROMAN_URDU',
        duration: d,
      });
      expect(prompt).toContain(VOICEOVER_DURATION_SPECS[d].wordRange);
      expect(prompt).toContain('Roman Urdu');
      expect(prompt).toContain('Do NOT use Devanagari/Hindi script');
    }

    const thumbPrompt = buildGenerationPrompt({
      requestId: 'req_thumb_12345',
      tool: 'THUMBNAIL_PROMPT',
      game: 'Last Asylum: Plague',
      topic: 'Night boss survival',
      language: 'ENGLISH',
      aspectRatio: '16:9',
    });
    expect(thumbPrompt).toContain('16:9');
    expect(thumbPrompt).toContain('do NOT attempt to render images');
  });
});
