export type AiToolType =
  | 'GAMING_TITLES'
  | 'HASHTAGS'
  | 'VOICEOVER_SCRIPT'
  | 'THUMBNAIL_PROMPT'
  | 'VIDEO_IDEAS';

export type OutputLanguage = 'ENGLISH' | 'ROMAN_URDU';

export type VoiceOverDuration = '15s' | '30s' | '60s' | '2m' | '5m' | '10m';

export interface QuotaStatus {
  limit: number;
  used: number;
  remaining: number;
  pktDate: string;
  resetsAtIso: string;
  timezone: 'Asia/Karachi';
}

export interface GenerateRequestInput {
  requestId: string;
  tool: AiToolType;
  game: string;
  topic: string;
  language: OutputLanguage;
  duration?: VoiceOverDuration | null;
  platformHint?: string | null;
  aspectRatio?: string | null;
}

export interface TranslateRequestInput {
  requestId: string;
  tool: AiToolType;
  game: string;
  currentContent: string;
  targetLanguage: OutputLanguage;
  duration?: VoiceOverDuration | null;
}

export interface VerifiedFirebaseIdentity {
  uid: string;
}

export interface D1PreparedStatementLike {
  bind(...values: unknown[]): D1PreparedStatementLike;
  first<T = Record<string, unknown>>(): Promise<T | null>;
  run(): Promise<{ meta?: { changes?: number } }>;
}

export interface D1DatabaseLike {
  prepare(query: string): D1PreparedStatementLike;
}

export interface BackendEnv {
  QUOTA_DB: D1DatabaseLike;
  GEMINI_API_KEY?: string;
  FIREBASE_PROJECT_ID?: string;
  GEMINI_MODEL?: string;
  DAILY_QUOTA_LIMIT?: string;
}
