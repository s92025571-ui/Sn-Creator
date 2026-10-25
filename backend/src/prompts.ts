import {
  GenerateRequestInput,
  OutputLanguage,
  TranslateRequestInput,
  VoiceOverDuration,
} from './types';

function languageDirective(language: OutputLanguage): string {
  if (language === 'ROMAN_URDU') {
    return [
      'OUTPUT LANGUAGE REQUIREMENT: Write all narration, titles, explanations, and creator copy in natural, fluent Roman Urdu (Urdu written strictly in the English/Latin alphabet, e.g., "Agar aap bhi final zone mein solo vs squad clutch karna chahte ho...").',
      'STRICT SCRIPT RULE: Do NOT use Devanagari/Hindi script and do NOT use Arabic/Nastaliq script. Keep common gaming terms (like zone, clutch, sensitivity, recoil, loadout, rush, squad, FPS) natural as Pakistani/South Asian gaming creators speak.',
    ].join('\n');
  }
  return 'OUTPUT LANGUAGE REQUIREMENT: Write in sharp, natural, modern English tailored to gaming content creators.';
}

interface DurationSpec {
  label: string;
  wordRange: string;
  structureRule: string;
}

export const VOICEOVER_DURATION_SPECS: Record<VoiceOverDuration, DurationSpec> = {
  '15s': {
    label: '15 seconds',
    wordRange: '35 to 45 spoken words',
    structureRule:
      'Ultra-tight Shorts/Reels sprint: 1-sentence immediate pattern-interrupt hook (0:00-0:03), 1 high-impact tip/moment (0:03-0:12), and 1 rapid CTA (0:12-0:15). Keep it strictly concise so a creator can speak it naturally in 15 seconds.',
  },
  '30s': {
    label: '30 seconds',
    wordRange: '70 to 85 spoken words',
    structureRule:
      'Punchy 30-second vertical video arc: Opening hook (0:00-0:04), fast problem/context setup (0:04-0:12), core tactical breakdown or highlight moment (0:12-0:25), and crisp closing CTA (0:25-0:30).',
  },
  '60s': {
    label: '60 seconds',
    wordRange: '140 to 165 spoken words',
    structureRule:
      'Complete 60-second YouTube Short / TikTok / Reel script: High-curiosity opening hook (0:00-0:05), 3 distinct fast-moving beats with [DELIVERY CUES] (0:05-0:50), and a memorable closing CTA (0:50-1:00).',
  },
  '2m': {
    label: '2 minutes',
    wordRange: '280 to 330 spoken words',
    structureRule:
      'Structured 2-minute gaming video narration: Timestamped Hook & Stakes (0:00-0:20), Segment 1 Setup/Mistake to Avoid (0:20-0:55), Segment 2 Core Tactical Execution (0:55-1:35), Segment 3 Pro Tip & Payoff (1:35-1:50), and Closing CTA (1:50-2:00). Include natural vocal pacing cues.',
  },
  '5m': {
    label: '5 minutes',
    wordRange: '700 to 825 spoken words',
    structureRule:
      'Substantive 5-minute full-length gaming creator script (~750 words — NEVER write an abbreviated summary or placeholder outline; write every spoken word in full!): [0:00-0:35] High-Retention Hook & Video Promise, [0:35-1:35] Chapter 1: Core Mechanics / Loadout / Context, [1:35-2:45] Chapter 2: Mid-Game Strategy & Positioning, [2:45-3:15] Mid-Video Retention Reset Hook, [3:15-4:30] Chapter 3: Advanced Clutch Execution & Common Mistakes, [4:30-5:00] Final Takeaway & Viewer Engagement CTA.',
  },
  '10m': {
    label: '10 minutes',
    wordRange: '1,350 to 1,600 spoken words',
    structureRule:
      'Comprehensive 10-minute flagship YouTube gaming episode script (~1,450 words — NEVER output a short summary or ask the user to expand sections; write the FULL ready-to-record spoken narration for every chapter!): [0:00-0:45] Cold Open & High-Stakes Hook, [0:45-2:15] Chapter 1: Deep Context & Meta Breakdown, [2:15-4:00] Chapter 2: Step-by-Step Setup / Early Game Execution, [4:00-5:45] Chapter 3: Mid-Game Rotations, Resource Control & Real Match Scenarios, [5:45-6:15] Mid-Episode Re-Hook, [6:15-8:00] Chapter 4: High-Pressure Endgame / Boss / Clutch Decisions, [8:00-9:20] Chapter 5: Pro Drills & Mistakes Most Players Make, [9:20-10:00] Strong Outro & Community Comment Question CTA.',
  },
};

export function buildSystemInstruction(): string {
  return [
    'You are SN Creator AI, a specialist production writer and strategist for gaming content creators across mobile, PC, and console titles.',
    'Always produce authentic, practical, ready-to-use gaming creator material tailored specifically to the requested game and topic.',
    'Never fabricate trend statistics, view-count guarantees, or fake algorithm promises.',
    'Format output cleanly with clear headings and bullet or numbered items that are easy to read and copy on an Android phone.',
  ].join('\n');
}

export function buildGenerationPrompt(input: GenerateRequestInput): string {
  const langRule = languageDirective(input.language);

  switch (input.tool) {
    case 'GAMING_TITLES':
      return [
        langRule,
        `GAME: ${input.game}`,
        `TOPIC / CONTEXT: ${input.topic}`,
        '',
        'TASK: Generate 9 distinct, compelling, game-specific video title suggestions grouped into 3 clear angles:',
        '1. High-Curiosity / Story Hook (3 titles)',
        '2. Tactical / Skill / Guide Angle (3 titles)',
        '3. High-Stakes / Challenge Angle (3 titles)',
        '',
        'RULES:',
        '- Tailor terminology specifically to the named game and topic.',
        '- Avoid repetitive templates or misleading clickbait.',
        '- Keep titles punchy (45–70 characters where practical) so they read cleanly on mobile feeds.',
      ].join('\n');

    case 'HASHTAGS':
      return [
        langRule,
        `GAME: ${input.game}`,
        `TOPIC / CONTEXT: ${input.topic}`,
        `TARGET PLATFORM: ${input.platformHint || 'YouTube, Shorts, TikTok, and Reels'}`,
        '',
        'TASK: Generate relevant gaming hashtags organized into:',
        '1. Core Game Hashtags (6–8 tags)',
        '2. Topic & Gameplay Niche Hashtags (8–10 tags)',
        '3. Creator & Platform Discovery Hashtags (5–6 tags)',
        '4. Ready-to-Paste Combined Block (a clean single block of the best 12–15 hashtags)',
        '',
        'STRICT HONESTY RULE: Do NOT fabricate view counts, trend percentages, or promise guaranteed virality.',
      ].join('\n');

    case 'VOICEOVER_SCRIPT': {
      const durationKey: VoiceOverDuration = input.duration || '60s';
      const spec = VOICEOVER_DURATION_SPECS[durationKey];
      return [
        langRule,
        `GAME: ${input.game}`,
        `TOPIC / CONTEXT: ${input.topic}`,
        `REQUESTED DURATION: ${spec.label}`,
        `TARGET SPOKEN LENGTH: ${spec.wordRange}`,
        '',
        'TASK: Write a complete, ready-to-record gaming creator voice-over script.',
        `PACING & STRUCTURE: ${spec.structureRule}`,
        '',
        'REQUIREMENTS:',
        '- Start with an immediate, scroll-stopping opening hook.',
        '- Write energetic, natural spoken narration (how a real gaming creator talks on mic, not stiff essay prose).',
        '- Include brief bracketed cues like [HOOK], [ENERGETIC], [PAUSE], or [ON SCREEN: ...] only where helpful.',
        '- Match the requested duration genuinely in word count and depth — never label a short script as a 5-minute or 10-minute script.',
        '- End with a natural, strong closing CTA.',
      ].join('\n');
    }

    case 'THUMBNAIL_PROMPT': {
      const ratio = input.aspectRatio || '16:9';
      return [
        langRule,
        `GAME: ${input.game}`,
        `TOPIC / CONTEXT: ${input.topic}`,
        `REQUESTED ASPECT RATIO: ${ratio}`,
        '',
        'TASK: Generate 3 detailed, ready-to-copy AI image-generation text prompts for gaming thumbnails (do NOT attempt to render images; output copy-ready text prompts only):',
        'Concept 1: High-Action Gameplay Focal Shot',
        'Concept 2: Dramatic Contrast / Stakes Split Composition',
        'Concept 3: Clean Iconic Subject + Bold Visual Hierarchy',
        '',
        'For EACH concept, provide:',
        '- Ready-to-Copy Prompt Block (explicitly describing foreground focal subject, character/gear expression, game-authentic environment, dramatic rim & volumetric lighting, color palette, depth of field, visual hierarchy, and aspect ratio ' +
          ratio +
          ')',
        '- Suggested 2–4 Word Thumbnail Text Overlay & Placement (where text sits so it never covers the focal face/action).',
      ].join('\n');
    }

    case 'VIDEO_IDEAS':
      return [
        langRule,
        `GAME: ${input.game}`,
        `TOPIC / CONTEXT: ${input.topic}`,
        '',
        'TASK: Generate 5 practical, distinct gaming video ideas tailored specifically to this game and topic.',
        'For EACH of the 5 ideas, include:',
        '- Idea Title / Concept',
        '- 5-Second Opening Hook (exact opening line or visual moment)',
        '- Practical Recording Approach (how the creator should capture gameplay/commentary on their device)',
        '- Viewer Appeal (why players of this game will click and watch without hype or fake guarantees).',
      ].join('\n');
  }
}

export function buildTranslationPrompt(input: TranslateRequestInput): string {
  const targetRule =
    input.targetLanguage === 'ROMAN_URDU'
      ? 'Translate the entire content below into natural, energetic Roman Urdu (Urdu written strictly in Latin letters, e.g., "Aaj hum baat karenge..."). NEVER use Hindi/Devanagari or Arabic/Nastaliq script. Keep gaming terms like clutch, recoil, squad, zone, sensitivity, and #hashtags natural.'
      : 'Translate the entire content below into natural, fluent, energetic gaming-creator English.';

  return [
    targetRule,
    `GAME CONTEXT: ${input.game}`,
    `TOOL TYPE: ${input.tool}`,
    input.duration ? `VOICE-OVER DURATION TARGET: ${input.duration}` : '',
    '',
    'RULES:',
    '- Preserve the exact structure, numbered items, timestamps, pacing length, and formatting of the original text.',
    '- Output ONLY the translated creator content without meta-commentary.',
    '',
    '--- CONTENT TO TRANSLATE ---',
    input.currentContent,
  ]
    .filter(Boolean)
    .join('\n');
}
