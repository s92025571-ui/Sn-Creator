import {
  AiToolType,
  GenerateRequestInput,
  OutputLanguage,
  TranslateRequestInput,
  VoiceOverDuration,
} from './types';

const VALID_TOOLS = new Set<AiToolType>([
  'GAMING_TITLES',
  'HASHTAGS',
  'VOICEOVER_SCRIPT',
  'THUMBNAIL_PROMPT',
  'VIDEO_IDEAS',
]);

const VALID_LANGUAGES = new Set<OutputLanguage>(['ENGLISH', 'ROMAN_URDU']);

const VALID_DURATIONS = new Set<VoiceOverDuration>([
  '15s',
  '30s',
  '60s',
  '2m',
  '5m',
  '10m',
]);

const REQUEST_ID_REGEX = /^[a-zA-Z0-9_-]{8,80}$/;

export const MAX_REQUEST_BYTES = 16_384; // 16 KB hard limit to prevent payload abuse

export class InputValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'InputValidationError';
  }
}

function cleanString(value: unknown, maxLen: number): string {
  if (typeof value !== 'string') return '';
  return value.trim().slice(0, maxLen);
}

export function validateRequestId(raw: unknown): string {
  if (typeof raw !== 'string') {
    throw new InputValidationError('Missing or invalid requestId.');
  }
  const trimmed = raw.trim();
  if (!REQUEST_ID_REGEX.test(trimmed)) {
    throw new InputValidationError(
      'Invalid requestId format (must be 8–80 alphanumeric, hyphen, or underscore characters).'
    );
  }
  return trimmed;
}

export function validateGenerateInput(body: unknown): GenerateRequestInput {
  if (!body || typeof body !== 'object') {
    throw new InputValidationError('Request body must be a JSON object.');
  }
  const obj = body as Record<string, unknown>;

  const requestId = validateRequestId(obj.requestId);
  const tool = obj.tool as AiToolType;
  if (!VALID_TOOLS.has(tool)) {
    throw new InputValidationError('Unsupported AI tool selected.');
  }

  const language = obj.language as OutputLanguage;
  if (!VALID_LANGUAGES.has(language)) {
    throw new InputValidationError('Output language must be ENGLISH or ROMAN_URDU.');
  }

  const game = typeof obj.game === 'string' ? obj.game.trim() : '';
  if (game.length < 2 || game.length > 80) {
    throw new InputValidationError('Game name must be between 2 and 80 characters.');
  }

  const topic = typeof obj.topic === 'string' ? obj.topic.trim() : '';
  if (topic.length < 3 || topic.length > 600) {
    throw new InputValidationError('Topic or context must be between 3 and 600 characters.');
  }

  let duration: VoiceOverDuration | null = null;
  if (tool === 'VOICEOVER_SCRIPT') {
    const rawDuration = (obj.duration ?? '60s') as VoiceOverDuration;
    if (!VALID_DURATIONS.has(rawDuration)) {
      throw new InputValidationError(
        'Voice-over duration must be one of: 15s, 30s, 60s, 2m, 5m, 10m.'
      );
    }
    duration = rawDuration;
  }

  const platformHint = cleanString(obj.platformHint, 60) || null;
  const aspectRatio = cleanString(obj.aspectRatio, 20) || null;

  return {
    requestId,
    tool,
    game,
    topic,
    language,
    duration,
    platformHint,
    aspectRatio,
  };
}

export function validateTranslateInput(body: unknown): TranslateRequestInput {
  if (!body || typeof body !== 'object') {
    throw new InputValidationError('Request body must be a JSON object.');
  }
  const obj = body as Record<string, unknown>;

  const requestId = validateRequestId(obj.requestId);
  const tool = obj.tool as AiToolType;
  if (!VALID_TOOLS.has(tool)) {
    throw new InputValidationError('Unsupported AI tool selected.');
  }

  const targetLanguage = obj.targetLanguage as OutputLanguage;
  if (!VALID_LANGUAGES.has(targetLanguage)) {
    throw new InputValidationError('Target language must be ENGLISH or ROMAN_URDU.');
  }

  const game = typeof obj.game === 'string' ? obj.game.trim() : '';
  if (game.length < 2 || game.length > 80) {
    throw new InputValidationError('Game name must be between 2 and 80 characters.');
  }

  const currentContent =
    typeof obj.currentContent === 'string' ? obj.currentContent.trim() : '';
  if (currentContent.length < 5 || currentContent.length > 14_000) {
    throw new InputValidationError(
      'Content to translate must be between 5 and 14,000 characters.'
    );
  }

  let duration: VoiceOverDuration | null = null;
  if (obj.duration && VALID_DURATIONS.has(obj.duration as VoiceOverDuration)) {
    duration = obj.duration as VoiceOverDuration;
  }

  return {
    requestId,
    tool,
    game,
    currentContent,
    targetLanguage,
    duration,
  };
}
