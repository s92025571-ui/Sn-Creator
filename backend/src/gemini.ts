import { GoogleGenAI } from '@google/genai';
import { buildSystemInstruction } from './prompts';

export const DEFAULT_FREE_GEMINI_MODEL = 'gemini-3.8-flash';
const GEMINI_TIMEOUT_MS = 25_000;

// Paid-only models are strictly blocked under the zero-paid-billing requirement.
const PROHIBITED_PAID_MODELS = new Set([
  'gemini-4-argon',
  'gemini-3-pro-image',
  'gemini-nano-banana-2.1',
  'gemini-3.1-flash-lite-image',
  'gemini-omni-1.1-flash',
  'lyria-3-clip-preview',
  'lyria-3-pro-preview',
]);

export type GeminiErrorCode =
  | 'SERVER_MISCONFIGURED'
  | 'GEMINI_QUOTA_EXHAUSTED'
  | 'GEMINI_FREE_TIER_UNAVAILABLE'
  | 'UPSTREAM_TIMEOUT'
  | 'UPSTREAM_ERROR';

export class GeminiServiceError extends Error {
  readonly code: GeminiErrorCode;
  readonly httpStatus: number;

  constructor(code: GeminiErrorCode, httpStatus: number, message: string) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
    this.name = 'GeminiServiceError';
  }
}

export interface AiGenerator {
  generateText(prompt: string, apiKey?: string, configuredModel?: string): Promise<{
    text: string;
    model: string;
  }>;
}

export const defaultGeminiGenerator: AiGenerator = {
  async generateText(
    prompt: string,
    apiKey?: string,
    configuredModel?: string
  ): Promise<{ text: string; model: string }> {
    const cleanKey = (apiKey ?? '').trim();
    if (!cleanKey || cleanKey === 'MY_GEMINI_API_KEY') {
      throw new GeminiServiceError(
        'SERVER_MISCONFIGURED',
        500,
        'Gemini API key secret is not configured on the backend.'
      );
    }

    const model = (configuredModel ?? '').trim() || DEFAULT_FREE_GEMINI_MODEL;
    if (PROHIBITED_PAID_MODELS.has(model)) {
      throw new GeminiServiceError(
        'GEMINI_FREE_TIER_UNAVAILABLE',
        503,
        'Configured model requires paid billing and is blocked by SN Creator zero-billing policy.'
      );
    }

    const ai = new GoogleGenAI({
      apiKey: cleanKey,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        },
      },
    });

    let timeoutHandle: ReturnType<typeof setTimeout> | undefined;
    const timeoutPromise = new Promise<never>((_, reject) => {
      timeoutHandle = setTimeout(() => {
        reject(
          new GeminiServiceError(
            'UPSTREAM_TIMEOUT',
            504,
            'Gemini AI request timed out. Your daily quota was not charged.'
          )
        );
      }, GEMINI_TIMEOUT_MS);
    });

    try {
      const response = await Promise.race([
        ai.models.generateContent({
          model,
          contents: prompt,
          config: {
            systemInstruction: buildSystemInstruction(),
            temperature: 0.78,
            topP: 0.92,
          },
        }),
        timeoutPromise,
      ]);

      const text = response.text?.trim() ?? '';
      if (!text) {
        throw new GeminiServiceError(
          'UPSTREAM_ERROR',
          502,
          'AI service returned an empty response. Your daily quota was not charged.'
        );
      }

      return { text, model };
    } catch (err) {
      if (err instanceof GeminiServiceError) throw err;

      const rawMessage = err instanceof Error ? err.message : String(err);
      const lower = rawMessage.toLowerCase();

      if (
        lower.includes('429') ||
        lower.includes('resource_exhausted') ||
        lower.includes('quota') ||
        lower.includes('rate limit')
      ) {
        throw new GeminiServiceError(
          'GEMINI_QUOTA_EXHAUSTED',
          429,
          'Gemini free-tier rate limit is temporarily exhausted. Your daily SN Creator quota was not charged.'
        );
      }

      if (
        lower.includes('403') ||
        lower.includes('404') ||
        lower.includes('api_key_invalid') ||
        lower.includes('billing') ||
        lower.includes('not found')
      ) {
        throw new GeminiServiceError(
          'GEMINI_FREE_TIER_UNAVAILABLE',
          503,
          'Gemini free-tier access is unavailable or misconfigured on the backend. Your daily quota was not charged.'
        );
      }

      if (lower.includes('timeout') || lower.includes('aborted')) {
        throw new GeminiServiceError(
          'UPSTREAM_TIMEOUT',
          504,
          'Gemini request timed out. Your daily quota was not charged.'
        );
      }

      throw new GeminiServiceError(
        'UPSTREAM_ERROR',
        502,
        'AI generation failed temporarily. Your daily quota was not charged.'
      );
    } finally {
      if (timeoutHandle !== undefined) {
        clearTimeout(timeoutHandle);
      }
    }
  },
};
