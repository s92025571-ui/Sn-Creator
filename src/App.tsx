import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { onAuthStateChanged, User } from 'firebase/auth';
import {
  ArrowLeft,
  Check,
  Copy,
  FileText,
  Gamepad2,
  Hash,
  HelpCircle,
  Image as ImageIcon,
  Lightbulb,
  LogOut,
  MessageCircle,
  Mic,
  RefreshCw,
  Search,
  Settings as SettingsIcon,
  Shield,
  Smartphone,
  Sparkles,
  Type as TypeIcon,
  WifiOff,
  AlertTriangle,
} from 'lucide-react';
import {
  auth,
  provisionedFirebaseMeta,
  signInWithGooglePopup,
  signOutUser,
} from './firebase';
import heroBannerImg from './assets/images/sn_creator_hero_banner_1792930236940.jpg';
import emblemImg from './assets/images/sn_creator_emblem_1792930238568.jpg';

type NavSection = 'home' | 'create' | 'help' | 'settings' | 'android';
type AiToolId =
  | 'GAMING_TITLES'
  | 'HASHTAGS'
  | 'VOICEOVER_SCRIPT'
  | 'THUMBNAIL_PROMPT'
  | 'VIDEO_IDEAS';
type OutputLanguage = 'ENGLISH' | 'ROMAN_URDU';
type VoiceDuration = '15s' | '30s' | '60s' | '2m' | '5m' | '10m';

interface SupportedGameOption {
  id: string;
  name: string;
  genre: string;
  requiresCustom?: boolean;
}

const SUPPORTED_GAMES: SupportedGameOption[] = [
  { id: 'pubg', name: 'PUBG Mobile', genre: 'Battle Royale · Tactical Squads' },
  { id: 'freefire', name: 'Free Fire', genre: 'Fast-Paced Battle Royale · Clash Squad' },
  { id: 'asylum', name: 'Last Asylum: Plague', genre: 'Survival Strategy · Outbreak Defense' },
  { id: 'gangstar', name: 'Gangstar Mirage City', genre: 'Open-World Action · Street Missions' },
  {
    id: 'other',
    name: 'Other Game',
    genre: 'Enter any custom mobile, PC, or console game',
    requiresCustom: true,
  },
];

interface ToolDescriptor {
  id: AiToolId;
  title: string;
  shortTitle: string;
  description: string;
  placeholder: string;
  icon: React.ComponentType<{ className?: string }>;
}

const AI_TOOLS: ToolDescriptor[] = [
  {
    id: 'GAMING_TITLES',
    title: 'Gaming Titles',
    shortTitle: 'Titles',
    description:
      'Compelling, game-specific video title suggestions organized into curiosity, guide, and challenge angles.',
    placeholder: 'e.g., Solo vs Squad 24 kills clutch in final Erangel zone with M416',
    icon: TypeIcon,
  },
  {
    id: 'HASHTAGS',
    title: 'Hashtags',
    shortTitle: 'Hashtags',
    description:
      'Broad, niche, and platform discovery gaming hashtags without fabricated trend guarantees.',
    placeholder: 'e.g., Ranked Clash Squad sniper montage for YouTube Shorts and TikTok',
    icon: Hash,
  },
  {
    id: 'VOICEOVER_SCRIPT',
    title: 'Voice-over Scripts',
    shortTitle: 'Voice-over',
    description:
      'Ready-to-record energetic creator scripts paced genuinely to your chosen duration preset.',
    placeholder: 'e.g., Top 5 sensitivity and zero-recoil settings after the latest patch',
    icon: Mic,
  },
  {
    id: 'THUMBNAIL_PROMPT',
    title: 'AI Thumbnail Prompts',
    shortTitle: 'Thumbnail Prompts',
    description:
      'Detailed copy-ready text prompts describing focal subject, lighting, text hierarchy, and aspect ratio.',
    placeholder: 'e.g., Intense night survival ambush against mutant horde, dramatic rim light',
    icon: ImageIcon,
  },
  {
    id: 'VIDEO_IDEAS',
    title: 'Gaming Video Ideas',
    shortTitle: 'Video Ideas',
    description:
      'Practical video concepts with a 5-second opening hook, recording approach, and viewer appeal.',
    placeholder: 'e.g., Creative challenge ideas to grow a new mobile gaming channel',
    icon: Lightbulb,
  },
];

const DURATION_OPTIONS: { value: VoiceDuration; label: string; detail: string }[] = [
  { value: '15s', label: '15 seconds', detail: '35–45 spoken words · Rapid Shorts hook & CTA' },
  { value: '30s', label: '30 seconds', detail: '70–85 spoken words · Fast setup & payoff' },
  { value: '60s', label: '60 seconds', detail: '140–165 spoken words · Full vertical video arc' },
  { value: '2m', label: '2 minutes', detail: '280–330 spoken words · 3 structured segments' },
  { value: '5m', label: '5 minutes', detail: '700–825 spoken words · Multi-chapter deep dive' },
  { value: '10m', label: '10 minutes', detail: '1,350–1,600 spoken words · Full episode script' },
];

interface QuotaInfo {
  limit: number;
  used: number;
  remaining: number;
  pktDate: string;
  resetsAtIso: string;
}

interface ActiveResult {
  tool: AiToolId;
  gameName: string;
  topic: string;
  language: OutputLanguage;
  duration: VoiceDuration | null;
  platformHint: string;
  aspectRatio: string;
  content: string;
}

const WHATSAPP_HELP_URL =
  'https://wa.me/923288501635?text=Hello%20SN%20Creator%20team%2C%20I%20need%20help%20with%20the%20app.';
const PREF_LANG_STORAGE_KEY = 'sn_creator_preferred_output_language';

function makeRequestId(): string {
  const rand =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}_${Math.random().toString(36).slice(2, 12)}`;
  return `req_${rand.replace(/[^a-zA-Z0-9_-]/g, '')}`;
}

function formatPktReset(iso: string): string {
  if (!iso) return '12:00 AM PKT (Asia/Karachi)';
  try {
    return (
      new Intl.DateTimeFormat('en-US', {
        timeZone: 'Asia/Karachi',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      }).format(new Date(iso)) + ' PKT'
    );
  } catch {
    return '12:00 AM PKT (Asia/Karachi)';
  }
}

export default function App() {
  const [showSplash, setShowSplash] = useState(true);
  const [authReady, setAuthReady] = useState(false);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const [isOnline, setIsOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [activeNav, setActiveNav] = useState<NavSection>('home');

  // Quota state
  const [quota, setQuota] = useState<QuotaInfo>({
    limit: 10,
    used: 0,
    remaining: 10,
    pktDate: '',
    resetsAtIso: '',
  });
  const [quotaLoading, setQuotaLoading] = useState(false);
  const [quotaError, setQuotaError] = useState<string | null>(null);

  // Language preference (affects generated output ONLY, never app UI language)
  const [preferredLanguage, setPreferredLanguage] = useState<OutputLanguage>(() => {
    try {
      const saved = localStorage.getItem(PREF_LANG_STORAGE_KEY);
      return saved === 'ROMAN_URDU' ? 'ROMAN_URDU' : 'ENGLISH';
    } catch {
      return 'ENGLISH';
    }
  });

  // Create form state
  const [selectedGameId, setSelectedGameId] = useState<string>('pubg');
  const [customGameName, setCustomGameName] = useState('');
  const [gameSearch, setGameSearch] = useState('');
  const [selectedTool, setSelectedTool] = useState<AiToolId>('GAMING_TITLES');
  const [topic, setTopic] = useState('');
  const [platformHint, setPlatformHint] = useState('YouTube & Shorts');
  const [aspectRatio, setAspectRatio] = useState('16:9');
  const [outputLanguage, setOutputLanguage] = useState<OutputLanguage>(preferredLanguage);
  const [duration, setDuration] = useState<VoiceDuration>('60s');

  // Active generation & transient result state (never persisted to storage/history)
  const [isGenerating, setIsGenerating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [activeResult, setActiveResult] = useState<ActiveResult | null>(null);
  const [viewingResult, setViewingResult] = useState(false);
  const [copiedNotice, setCopiedNotice] = useState(false);
  const [legalDialog, setLegalDialog] = useState<'privacy' | 'terms' | 'contact' | null>(null);
  const [heroImgFailed, setHeroImgFailed] = useState(false);
  const [emblemImgFailed, setEmblemImgFailed] = useState(false);
  const [lastFailedAction, setLastFailedAction] = useState<
    'generate' | 'regenerate' | 'translate' | null
  >(null);

  const inFlightRef = useRef(false);

  // Brief, polished 3D splash transition
  useEffect(() => {
    const timer = window.setTimeout(() => setShowSplash(false), 1250);
    return () => window.clearTimeout(timer);
  }, []);

  // Online / Offline listener
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const fetchQuota = useCallback(async (userObj: User | null) => {
    if (!userObj) return;
    if (!navigator.onLine) {
      setIsOnline(false);
      setQuotaError('Internet connection required for AI generation.');
      return;
    }
    setQuotaLoading(true);
    setQuotaError(null);
    try {
      const token = await userObj.getIdToken();
      const res = await fetch('/api/v1/quota', {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data?.message || 'Unable to load daily quota.');
      }
      if (data?.quota) {
        setQuota(data.quota);
      }
    } catch (err) {
      setQuotaError(
        err instanceof Error ? err.message : 'Could not load daily quota.'
      );
    } finally {
      setQuotaLoading(false);
    }
  }, []);

  // Observe real Firebase Google Auth state
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      setAuthReady(true);
      if (user) {
        setAuthError(null);
        void fetchQuota(user);
      } else {
        setActiveResult(null);
        setViewingResult(false);
      }
    });
    return () => unsub();
  }, [fetchQuota]);

  const handleGoogleSignIn = async () => {
    if (authBusy) return;
    if (!navigator.onLine) {
      setIsOnline(false);
      setAuthError('Internet connection required for Google Sign-In.');
      return;
    }
    setAuthBusy(true);
    setAuthError(null);
    try {
      await signInWithGooglePopup();
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      if (msg.includes('popup-closed-by-user') || msg.includes('cancelled')) {
        setAuthError('Google sign-in was cancelled. Sign in with Google to continue.');
      } else if (msg.includes('network-request-failed')) {
        setAuthError('Network error during Google sign-in. Check your connection and retry.');
      } else {
        setAuthError('Google authentication failed. Please retry.');
      }
    } finally {
      setAuthBusy(false);
    }
  };

  const handleSignOut = async () => {
    await signOutUser();
    setActiveResult(null);
    setViewingResult(false);
    setActiveNav('home');
  };

  const handlePreferredLanguageChange = (lang: OutputLanguage) => {
    setPreferredLanguage(lang);
    setOutputLanguage(lang);
    try {
      localStorage.setItem(PREF_LANG_STORAGE_KEY, lang);
    } catch {
      // Ignore storage restriction
    }
  };

  const selectedGameObj = useMemo(
    () => SUPPORTED_GAMES.find((g) => g.id === selectedGameId) ?? SUPPORTED_GAMES[0],
    [selectedGameId]
  );

  const filteredGames = useMemo(() => {
    const q = gameSearch.trim().toLowerCase();
    if (!q) return SUPPORTED_GAMES;
    return SUPPORTED_GAMES.filter(
      (g) =>
        g.name.toLowerCase().includes(q) ||
        g.genre.toLowerCase().includes(q) ||
        g.requiresCustom
    );
  }, [gameSearch]);

  const selectedToolObj = useMemo(
    () => AI_TOOLS.find((t) => t.id === selectedTool) ?? AI_TOOLS[0],
    [selectedTool]
  );

  const executeGenerate = async (mode: 'generate' | 'regenerate') => {
    if (inFlightRef.current || isGenerating || !currentUser) return;
    if (!navigator.onLine) {
      setIsOnline(false);
      setActionError('Internet connection required for AI generation.');
      setLastFailedAction(mode);
      return;
    }
    if (quota.remaining <= 0) {
      setActionError(
        `You have used all 10 daily generations. Quota resets at ${formatPktReset(quota.resetsAtIso)}.`
      );
      return;
    }

    const targetTool = mode === 'regenerate' && activeResult ? activeResult.tool : selectedTool;
    const targetGameName =
      mode === 'regenerate' && activeResult
        ? activeResult.gameName
        : selectedGameObj.requiresCustom
          ? customGameName.trim()
          : selectedGameObj.name;
    const targetTopic =
      mode === 'regenerate' && activeResult ? activeResult.topic : topic.trim();
    const targetLanguage =
      mode === 'regenerate' && activeResult ? activeResult.language : outputLanguage;
    const targetDuration =
      targetTool === 'VOICEOVER_SCRIPT'
        ? mode === 'regenerate' && activeResult?.duration
          ? activeResult.duration
          : duration
        : null;
    const targetPlatform =
      mode === 'regenerate' && activeResult ? activeResult.platformHint : platformHint;
    const targetRatio =
      mode === 'regenerate' && activeResult ? activeResult.aspectRatio : aspectRatio;

    if (targetGameName.length < 2) {
      setActionError('Please enter your custom game name (at least 2 characters).');
      return;
    }
    if (targetTopic.length < 3) {
      setActionError('Please enter a gaming topic or context (at least 3 characters).');
      return;
    }

    inFlightRef.current = true;
    setIsGenerating(true);
    setActionError(null);
    setLastFailedAction(mode);

    try {
      const token = await currentUser.getIdToken();
      const res = await fetch('/api/v1/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          requestId: makeRequestId(),
          tool: targetTool,
          game: targetGameName,
          topic: targetTopic,
          language: targetLanguage,
          duration: targetDuration,
          platformHint: targetTool === 'HASHTAGS' ? targetPlatform : null,
          aspectRatio: targetTool === 'THUMBNAIL_PROMPT' ? targetRatio : null,
        }),
      });

      const data = await res.json();
      if (data?.quota) {
        setQuota(data.quota);
      }

      if (!res.ok) {
        throw new Error(
          data?.message || 'AI generation failed. Your daily quota was not charged.'
        );
      }

      setActiveResult({
        tool: targetTool,
        gameName: targetGameName,
        topic: targetTopic,
        language: targetLanguage,
        duration: targetDuration,
        platformHint: targetPlatform,
        aspectRatio: targetRatio,
        content: data.content,
      });
      setViewingResult(true);
      setLastFailedAction(null);
    } catch (err) {
      setActionError(
        err instanceof Error
          ? err.message
          : 'Request failed. Your daily quota was not charged.'
      );
    } finally {
      setIsGenerating(false);
      inFlightRef.current = false;
    }
  };

  const executeSwitchLanguage = async () => {
    if (inFlightRef.current || isGenerating || !currentUser || !activeResult) return;
    if (!navigator.onLine) {
      setIsOnline(false);
      setActionError('Internet connection required for AI generation.');
      setLastFailedAction('translate');
      return;
    }
    if (quota.remaining <= 0) {
      setActionError(
        `You have used all 10 daily generations. Quota resets at ${formatPktReset(quota.resetsAtIso)}.`
      );
      return;
    }

    const nextLanguage: OutputLanguage =
      activeResult.language === 'ENGLISH' ? 'ROMAN_URDU' : 'ENGLISH';

    inFlightRef.current = true;
    setIsGenerating(true);
    setActionError(null);
    setLastFailedAction('translate');

    try {
      const token = await currentUser.getIdToken();
      const res = await fetch('/api/v1/translate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          requestId: makeRequestId(),
          tool: activeResult.tool,
          game: activeResult.gameName,
          currentContent: activeResult.content,
          targetLanguage: nextLanguage,
          duration: activeResult.duration,
        }),
      });

      const data = await res.json();
      if (data?.quota) {
        setQuota(data.quota);
      }

      if (!res.ok) {
        throw new Error(
          data?.message || 'Language switch failed. Your daily quota was not charged.'
        );
      }

      setActiveResult({
        ...activeResult,
        language: nextLanguage,
        content: data.content,
      });
      setLastFailedAction(null);
    } catch (err) {
      setActionError(
        err instanceof Error
          ? err.message
          : 'Translation failed. Your daily quota was not charged.'
      );
    } finally {
      setIsGenerating(false);
      inFlightRef.current = false;
    }
  };

  const handleCopyOutput = async () => {
    if (!activeResult) return;
    try {
      await navigator.clipboard.writeText(activeResult.content);
      setCopiedNotice(true);
      window.setTimeout(() => setCopiedNotice(false), 2200);
    } catch {
      setActionError('Clipboard permission was denied by your browser.');
    }
  };

  const handleRetry = () => {
    if (lastFailedAction === 'translate') {
      void executeSwitchLanguage();
    } else if (lastFailedAction === 'regenerate') {
      void executeGenerate('regenerate');
    } else {
      void executeGenerate('generate');
    }
  };

  const displayName =
    currentUser?.displayName?.trim() ||
    currentUser?.email?.split('@')[0] ||
    'Creator';

  // 1. Animated Splash Screen
  if (showSplash || !authReady) {
    return (
      <div className="min-h-screen bg-[#050509] flex flex-col items-center justify-center px-6">
        <div
          className="relative w-32 h-32 rounded-3xl border border-purple-500/40 bg-[#120d24] flex items-center justify-center shadow-[0_0_60px_rgba(168,85,247,0.32)] transition-transform duration-700"
          style={{ transform: 'perspective(500px) rotateY(-12deg)' }}
        >
          {!emblemImgFailed ? (
            <img
              src={emblemImg}
              alt="SN Creator 3D Emblem"
              referrerPolicy="no-referrer"
              onError={() => setEmblemImgFailed(true)}
              className="w-24 h-24 object-contain rounded-2xl"
            />
          ) : (
            <span className="text-4xl font-extrabold tracking-tight text-purple-300">
              SN
            </span>
          )}
        </div>
        <h1 className="mt-6 text-3xl font-extrabold tracking-tight text-slate-50">
          SN Creator
        </h1>
        <p className="mt-1.5 text-sm text-slate-400">AI Gaming Content Studio</p>
      </div>
    );
  }

  // 2. Authentication Screen (No Guest Access)
  if (!currentUser) {
    return (
      <div className="min-h-screen bg-[#050509] text-slate-50 flex flex-col">
        <header className="flex items-center justify-between gap-8 px-6 lg:px-12 py-4 border-b border-white/10">
          <a
            href="#top"
            className="text-lg font-extrabold tracking-tight text-slate-50 whitespace-nowrap shrink-0"
          >
            SN Creator
          </a>
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-400">
            <a
              href="#features"
              className="hover:text-slate-100 transition-colors whitespace-nowrap shrink-0"
            >
              Five AI Tools
            </a>
            <a
              href="#games"
              className="hover:text-slate-100 transition-colors whitespace-nowrap shrink-0"
            >
              Supported Games
            </a>
            <a
              href={WHATSAPP_HELP_URL}
              target="_blank"
              rel="noreferrer"
              className="hover:text-slate-100 transition-colors whitespace-nowrap shrink-0"
            >
              Support
            </a>
          </nav>
          <div className="shrink-0">
            <button
              type="button"
              onClick={handleGoogleSignIn}
              disabled={authBusy}
              className="px-4 py-2 text-xs font-semibold text-slate-950 bg-slate-50 hover:bg-purple-100 rounded-lg transition-colors whitespace-nowrap shrink-0 disabled:opacity-60"
            >
              {authBusy ? 'Signing in...' : 'Continue with Google'}
            </button>
          </div>
        </header>

        <main className="flex-1 max-w-6xl w-full mx-auto px-6 lg:px-12 py-12 lg:py-20 grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
          <div className="lg:col-span-6 space-y-6">
            <div className="w-16 h-16 rounded-2xl border border-purple-500/35 bg-[#120d24] flex items-center justify-center shadow-[0_0_36px_rgba(147,51,234,0.25)]">
              {!emblemImgFailed ? (
                <img
                  src={emblemImg}
                  alt="SN Creator Emblem"
                  referrerPolicy="no-referrer"
                  onError={() => setEmblemImgFailed(true)}
                  className="w-12 h-12 object-contain rounded-xl"
                />
              ) : (
                <span className="text-xl font-extrabold text-purple-300">SN</span>
              )}
            </div>

            <h1 className="text-4xl lg:text-5xl font-extrabold tracking-tight text-slate-50 leading-tight [text-wrap:balance]">
              Welcome to SN Creator
            </h1>

            <p className="text-base text-slate-300 leading-relaxed max-w-xl">
              Generate game-specific titles, platform hashtags, duration-paced voice-over
              scripts, copy-ready thumbnail prompts, and fresh video ideas in English or
              Roman Urdu. Includes full native Kotlin Android source and automated GitHub
              Actions APK builds.
            </p>

            <div className="p-6 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-4 max-w-md">
              <div className="text-sm text-slate-300">
                Sign in with your Google account to unlock 10 free AI generations per day
                (resets at midnight Pakistan time). Guest access is disabled.
              </div>

              <button
                type="button"
                onClick={handleGoogleSignIn}
                disabled={authBusy}
                className="w-full min-h-[48px] px-5 py-3 rounded-xl bg-slate-50 hover:bg-purple-100 text-slate-950 font-bold text-sm flex items-center justify-center gap-3 transition-colors disabled:opacity-60"
              >
                <span className="w-6 h-6 rounded-full bg-purple-900 text-purple-200 text-xs font-extrabold flex items-center justify-center">
                  G
                </span>
                <span>{authBusy ? 'Authenticating with Google...' : 'Continue with Google'}</span>
              </button>

              {authError && (
                <div className="p-4 rounded-xl bg-[#261119] border border-red-500/40 space-y-3">
                  <div className="flex items-center gap-2 text-sm font-semibold text-red-300">
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    <span>Authentication Failed</span>
                  </div>
                  <p className="text-xs text-slate-200 leading-relaxed">{authError}</p>
                  <button
                    type="button"
                    onClick={handleGoogleSignIn}
                    className="px-3.5 py-1.5 rounded-lg border border-purple-400/40 text-xs font-semibold text-slate-100 hover:bg-white/5 transition-colors"
                  >
                    Retry Sign-In
                  </button>
                </div>
              )}
            </div>

            <div id="games" className="text-xs text-slate-400 pt-2">
              PUBG Mobile · Free Fire · Last Asylum: Plague · Gangstar Mirage City · Custom Games
            </div>
          </div>

          <div className="lg:col-span-6">
            <div className="rounded-2xl overflow-hidden border border-purple-500/25 bg-[#0e0c18] shadow-[0_20px_70px_rgba(88,28,135,0.28)]">
              {!heroImgFailed ? (
                <div className="relative aspect-video">
                  <img
                    src={heroImgImgOrFallback(heroBannerImg)}
                    alt="SN Creator mobile esports production studio"
                    referrerPolicy="no-referrer"
                    onError={() => setHeroImgFailed(true)}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#050509] via-black/30 to-transparent" />
                  <div className="absolute bottom-5 left-5 right-5">
                    <p className="text-sm font-semibold text-slate-100">
                      Built for Mobile & Esports Content Creators
                    </p>
                    <p className="text-xs text-slate-300 mt-1">
                      Kotlin · Jetpack Compose Material 3 · Firebase Google Auth · Zero-Billing Backend
                    </p>
                  </div>
                </div>
              ) : (
                <div className="aspect-video flex flex-col items-center justify-center p-8 bg-gradient-to-br from-[#1b1033] to-[#08070f]">
                  <Gamepad2 className="w-12 h-12 text-purple-400 mb-3" />
                  <p className="text-sm font-semibold text-slate-100">
                    SN Creator Gaming Production Suite
                  </p>
                </div>
              )}

              <div id="features" className="p-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
                {AI_TOOLS.slice(0, 4).map((t) => {
                  const Icon = t.icon;
                  return (
                    <div key={t.id} className="space-y-1">
                      <div className="flex items-center gap-2 text-sm font-semibold text-slate-100">
                        <Icon className="w-4 h-4 text-purple-400 shrink-0" />
                        <span>{t.title}</span>
                      </div>
                      <p className="text-xs text-slate-400 leading-relaxed">
                        {t.description}
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </main>
      </div>
    );
  }

  // 3. Authenticated Studio Shell (Top Bar + Mobile Bottom Nav + Active Section / Result Screen)
  return (
    <div className="min-h-screen bg-[#050509] text-slate-50 flex flex-col pb-20 md:pb-0">
      {/* Strict 3-Zone Top Bar Contract */}
      <header className="sticky top-0 z-30 bg-[#080711]/90 backdrop-blur-md flex items-center justify-between gap-8 px-6 lg:px-12 py-3.5 border-b border-white/10">
        <button
          type="button"
          onClick={() => {
            setViewingResult(false);
            setActiveNav('home');
          }}
          className="text-lg font-extrabold tracking-tight text-slate-50 whitespace-nowrap shrink-0 cursor-pointer"
        >
          SN Creator
        </button>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-400">
          {(
            [
              ['home', 'Home'],
              ['create', 'Create'],
              ['help', 'Get Help'],
              ['settings', 'Settings'],
              ['android', 'Android APK'],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              onClick={() => {
                setViewingResult(false);
                setActionError(null);
                setActiveNav(id);
              }}
              className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                activeNav === id && !viewingResult
                  ? 'text-purple-300 underline underline-offset-8 decoration-purple-400'
                  : 'hover:text-slate-100'
              }`}
            >
              {label}
            </button>
          ))}
        </nav>

        <div className="shrink-0">
          <button
            type="button"
            onClick={() => {
              setViewingResult(false);
              setActiveNav('create');
            }}
            className="px-4 py-2 text-xs font-semibold text-white bg-purple-600 hover:bg-purple-500 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
          >
            Create Content
          </button>
        </div>
      </header>

      <main className="flex-1 max-w-6xl w-full mx-auto px-5 lg:px-12 py-8">
        {!isOnline && (
          <div className="mb-6 p-4 rounded-xl bg-[#261119] border border-red-500/40 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <WifiOff className="w-5 h-5 text-red-400 shrink-0" />
              <div>
                <p className="text-sm font-semibold text-slate-100">
                  Internet connection required for AI generation.
                </p>
                <p className="text-xs text-slate-300">
                  Reconnect to the internet and tap Retry.
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => {
                setIsOnline(navigator.onLine);
                if (lastFailedAction) {
                  handleRetry();
                } else {
                  void fetchQuota(currentUser);
                }
              }}
              className="px-4 py-2 rounded-lg border border-purple-400/40 text-xs font-semibold text-slate-100 hover:bg-white/5 whitespace-nowrap shrink-0"
            >
              Retry
            </button>
          </div>
        )}

        {/* Active Result Screen */}
        {viewingResult && activeResult ? (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="flex items-center justify-between gap-4">
              <button
                type="button"
                disabled={isGenerating}
                onClick={() => {
                  setViewingResult(false);
                  setActionError(null);
                }}
                className="min-h-[44px] px-3 py-2 rounded-xl border border-white/10 hover:bg-white/5 text-sm font-medium text-slate-200 flex items-center gap-2 transition-colors whitespace-nowrap shrink-0 disabled:opacity-50"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back to Create</span>
              </button>

              <div className="text-xs text-slate-400 font-mono tabular-nums text-right">
                {quota.remaining}/{quota.limit} generations left · Not saved as history
              </div>
            </div>

            <div className="space-y-1">
              <h1 className="text-2xl font-bold text-slate-50">
                {AI_TOOLS.find((t) => t.id === activeResult.tool)?.title}
              </h1>
              <p className="text-xs text-purple-300">
                {activeResult.gameName} ·{' '}
                {activeResult.language === 'ROMAN_URDU' ? 'Roman Urdu' : 'English'}
                {activeResult.duration ? ` · ${activeResult.duration}` : ''}
              </p>
            </div>

            {isGenerating && (
              <div className="p-4 rounded-xl bg-[#161229] border border-purple-500/40 flex items-center gap-3">
                <RefreshCw className="w-5 h-5 text-purple-400 animate-spin shrink-0" />
                <div className="text-sm text-slate-200">
                  Generating fresh AI response... Your active text stays visible until ready.
                </div>
              </div>
            )}

            {actionError && (
              <div className="p-4 rounded-xl bg-[#261119] border border-red-500/40 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <p className="text-sm text-slate-200">{actionError}</p>
                {quota.remaining > 0 && (
                  <button
                    type="button"
                    onClick={handleRetry}
                    className="px-3.5 py-2 rounded-lg border border-purple-400/40 text-xs font-semibold text-slate-100 hover:bg-white/5 whitespace-nowrap shrink-0"
                  >
                    Retry
                  </button>
                )}
              </div>
            )}

            {copiedNotice && (
              <div className="p-3.5 rounded-xl bg-emerald-950/60 border border-emerald-500/40 flex items-center gap-2 text-xs text-emerald-200">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>
                  Copied to clipboard. Paste it into your notes — SN Creator does not persist
                  content history.
                </span>
              </div>
            )}

            <div className="p-6 rounded-2xl bg-[#110f1d] border border-purple-500/30">
              <pre className="whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-slate-100 select-text">
                {activeResult.content}
              </pre>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <button
                type="button"
                onClick={handleCopyOutput}
                className="min-h-[48px] px-4 py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-semibold text-sm flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
              >
                {copiedNotice ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                <span>{copiedNotice ? 'Copied!' : 'Copy Output'}</span>
              </button>

              <button
                type="button"
                disabled={isGenerating || quota.remaining <= 0}
                onClick={() => void executeGenerate('regenerate')}
                className="min-h-[48px] px-4 py-3 rounded-xl border border-purple-500/30 bg-[#131122] hover:bg-[#1b1730] text-slate-100 font-semibold text-sm flex items-center justify-center gap-2 transition-colors whitespace-nowrap disabled:opacity-50"
              >
                <RefreshCw className="w-4 h-4" />
                <span>Regenerate</span>
              </button>

              <button
                type="button"
                disabled={isGenerating || quota.remaining <= 0}
                onClick={() => void executeSwitchLanguage()}
                className="min-h-[48px] px-4 py-3 rounded-xl border border-purple-400/50 bg-[#131122] hover:bg-[#1b1730] text-purple-200 font-semibold text-sm flex items-center justify-center gap-2 transition-colors whitespace-nowrap disabled:opacity-50"
              >
                <Sparkles className="w-4 h-4" />
                <span>
                  Switch to{' '}
                  {activeResult.language === 'ENGLISH' ? 'Roman Urdu' : 'English'}
                </span>
              </button>
            </div>
          </div>
        ) : activeNav === 'home' ? (
          /* HOME VIEW */
          <div className="space-y-10">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
              <div className="lg:col-span-7 p-6 lg:p-8 rounded-2xl bg-[#110f1d] border border-purple-500/30 flex flex-col justify-between space-y-6">
                <div className="space-y-2">
                  <p className="text-xs font-medium text-purple-300">
                    Signed in with Google · Pakistan Time Daily Quota
                  </p>
                  <h1 className="text-3xl font-extrabold tracking-tight text-slate-50 [text-wrap:balance]">
                    Welcome back, {displayName}
                  </h1>
                  <p className="text-sm text-slate-300 leading-relaxed">
                    Pick a supported game or custom title and generate high-retention titles,
                    hashtags, duration-accurate voice-over scripts, thumbnail prompts, or video
                    concepts.
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-4">
                  <button
                    type="button"
                    onClick={() => setActiveNav('create')}
                    className="min-h-[48px] px-6 py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-sm flex items-center gap-2 transition-colors whitespace-nowrap"
                  >
                    <Sparkles className="w-4 h-4" />
                    <span>Create Content</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setActiveNav('android')}
                    className="min-h-[48px] px-5 py-3 rounded-xl border border-purple-500/30 hover:bg-white/5 text-slate-200 font-semibold text-sm flex items-center gap-2 transition-colors whitespace-nowrap"
                  >
                    <Smartphone className="w-4 h-4 text-purple-300" />
                    <span>Android APK & GitHub Guide</span>
                  </button>
                </div>
              </div>

              <div className="lg:col-span-5 p-6 lg:p-8 rounded-2xl bg-[#110f1d] border border-purple-500/25 flex flex-col justify-between space-y-5">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-200">
                    Daily Generation Counter
                  </span>
                  <button
                    type="button"
                    onClick={() => void fetchQuota(currentUser)}
                    disabled={quotaLoading}
                    className="p-2 rounded-lg hover:bg-white/5 text-slate-400 hover:text-slate-100 transition-colors"
                    title="Refresh quota"
                  >
                    <RefreshCw
                      className={`w-4 h-4 ${quotaLoading ? 'animate-spin' : ''}`}
                    />
                  </button>
                </div>

                <div>
                  <div className="text-3xl font-extrabold font-mono tabular-nums text-slate-50">
                    {quota.remaining}/{quota.limit} generations left
                  </div>
                  <div className="mt-3 w-full h-2.5 rounded-full bg-[#241f38] overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        quota.remaining === 0
                          ? 'bg-red-500'
                          : quota.remaining <= 3
                            ? 'bg-amber-500'
                            : 'bg-purple-500'
                      }`}
                      style={{
                        width: `${Math.max(0, Math.min(100, (quota.remaining / quota.limit) * 100))}%`,
                      }}
                    />
                  </div>
                </div>

                {quotaError ? (
                  <p className="text-xs text-red-300">{quotaError}</p>
                ) : (
                  <p className="text-xs text-slate-400 leading-relaxed">
                    Resets at {formatPktReset(quota.resetsAtIso)} (Asia/Karachi). Failed or
                    timed-out requests never deduct from your quota.
                  </p>
                )}
              </div>
            </div>

            {/* Quick Access: Supported Games */}
            <section className="space-y-4">
              <div className="flex items-baseline justify-between gap-4">
                <h2 className="text-xl font-bold text-slate-50">Supported Games</h2>
                <span className="text-xs text-slate-400">
                  Select a title to open Create
                </span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
                {SUPPORTED_GAMES.map((game) => (
                  <button
                    key={game.id}
                    type="button"
                    onClick={() => {
                      setSelectedGameId(game.id);
                      setActiveNav('create');
                    }}
                    className="p-4 rounded-xl bg-[#110f1d] hover:bg-[#19152b] border border-purple-500/20 hover:border-purple-500/50 text-left transition-colors space-y-1.5 cursor-pointer"
                  >
                    <div className="flex items-center gap-2 text-sm font-bold text-slate-100">
                      <Gamepad2 className="w-4 h-4 text-purple-400 shrink-0" />
                      <span className="truncate">{game.name}</span>
                    </div>
                    <p className="text-xs text-slate-400 line-clamp-2">{game.genre}</p>
                  </button>
                ))}
              </div>
            </section>

            {/* Quick Access: Five AI Tools */}
            <section className="space-y-4">
              <h2 className="text-xl font-bold text-slate-50">Five AI Gaming Tools</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {AI_TOOLS.map((tool) => {
                  const Icon = tool.icon;
                  return (
                    <button
                      key={tool.id}
                      type="button"
                      onClick={() => {
                        setSelectedTool(tool.id);
                        setActiveNav('create');
                      }}
                      className="p-5 rounded-2xl bg-[#110f1d] hover:bg-[#19152b] border border-purple-500/20 hover:border-purple-500/50 text-left transition-colors space-y-2.5 cursor-pointer"
                    >
                      <div className="w-10 h-10 rounded-xl bg-purple-950/80 border border-purple-500/30 flex items-center justify-center">
                        <Icon className="w-5 h-5 text-purple-300" />
                      </div>
                      <h3 className="text-base font-bold text-slate-100">{tool.title}</h3>
                      <p className="text-xs text-slate-400 leading-relaxed">
                        {tool.description}
                      </p>
                    </button>
                  );
                })}
              </div>
            </section>

            {/* Contact / Help Shortcut */}
            <section className="p-6 rounded-2xl bg-[#110f1d] border border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <h3 className="text-base font-bold text-slate-100">
                  Need assistance with SN Creator?
                </h3>
                <p className="text-xs text-slate-400">
                  Read the creator workflow guide or reach our team directly on WhatsApp.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActiveNav('help')}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-purple-500/35 hover:bg-white/5 text-xs font-semibold text-purple-200 whitespace-nowrap shrink-0 cursor-pointer"
              >
                Open Get Help
              </button>
            </section>
          </div>
        ) : activeNav === 'create' ? (
          /* CREATE VIEW */
          <div className="max-w-4xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-2">
              <div>
                <h1 className="text-2xl font-extrabold text-slate-50">Create Gaming Content</h1>
                <p className="text-sm text-slate-400">
                  Select your game, choose one of the five AI tools, and generate ready-to-use
                  copy.
                </p>
              </div>
              <div className="text-xs font-mono tabular-nums text-purple-300">
                {quota.remaining}/{quota.limit} generations left today
              </div>
            </div>

            {/* Step 1: Searchable / Selectable Game */}
            <div className="p-6 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <label className="text-sm font-bold text-slate-100">1. Select Game</label>
                <div className="relative w-full sm:w-64">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={gameSearch}
                    onChange={(e) => setGameSearch(e.target.value)}
                    placeholder="Filter games..."
                    disabled={isGenerating}
                    className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#090812] border border-white/10 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-purple-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-5 gap-2.5">
                {filteredGames.map((g) => {
                  const active = selectedGameId === g.id;
                  return (
                    <button
                      key={g.id}
                      type="button"
                      disabled={isGenerating}
                      onClick={() => {
                        setSelectedGameId(g.id);
                        setActionError(null);
                      }}
                      className={`min-h-[44px] px-3.5 py-2.5 rounded-xl border text-left transition-colors cursor-pointer ${
                        active
                          ? 'bg-purple-950/70 border-purple-400 text-slate-50'
                          : 'bg-[#090812] border-white/10 text-slate-300 hover:border-purple-500/40'
                      }`}
                    >
                      <div className="text-xs font-bold truncate">{g.name}</div>
                    </button>
                  );
                })}
              </div>

              {selectedGameObj.requiresCustom && (
                <div className="pt-2 space-y-1.5">
                  <label className="block text-xs font-semibold text-purple-300">
                    Custom Game Name (Required)
                  </label>
                  <input
                    type="text"
                    value={customGameName}
                    onChange={(e) => {
                      setCustomGameName(e.target.value.slice(0, 80));
                      setActionError(null);
                    }}
                    disabled={isGenerating}
                    placeholder="e.g., Valorant Mobile, Call of Duty Mobile, Minecraft"
                    className="w-full px-4 py-3 rounded-xl bg-[#090812] border border-purple-500/40 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-purple-400"
                  />
                </div>
              )}
            </div>

            {/* Step 2: Five AI Tool Cards */}
            <div className="space-y-3">
              <div className="text-sm font-bold text-slate-100">2. Select AI Tool</div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {AI_TOOLS.map((tool) => {
                  const Icon = tool.icon;
                  const active = selectedTool === tool.id;
                  return (
                    <button
                      key={tool.id}
                      type="button"
                      disabled={isGenerating}
                      onClick={() => {
                        setSelectedTool(tool.id);
                        setActionError(null);
                      }}
                      className={`p-4 rounded-2xl border text-left transition-colors space-y-2 cursor-pointer ${
                        active
                          ? 'bg-[#1b1433] border-purple-400'
                          : 'bg-[#110f1d] border-white/10 hover:border-purple-500/40'
                      }`}
                    >
                      <div className="flex items-center gap-2.5">
                        <Icon
                          className={`w-4 h-4 shrink-0 ${
                            active ? 'text-purple-300' : 'text-slate-400'
                          }`}
                        />
                        <span className="text-sm font-bold text-slate-100">{tool.title}</span>
                      </div>
                      <p className="text-xs text-slate-400 leading-relaxed">
                        {tool.description}
                      </p>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Conditional Voice-over Duration Selector (ONLY for Voice-over Scripts) */}
            {selectedTool === 'VOICEOVER_SCRIPT' && (
              <div className="p-6 rounded-2xl bg-[#141026] border border-purple-500/40 space-y-3">
                <div>
                  <div className="text-sm font-bold text-slate-100">
                    Voice-over Duration Preset
                  </div>
                  <p className="text-xs text-purple-300 mt-0.5">
                    {DURATION_OPTIONS.find((d) => d.value === duration)?.detail}
                  </p>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
                  {DURATION_OPTIONS.map((opt) => {
                    const active = duration === opt.value;
                    return (
                      <button
                        key={opt.value}
                        type="button"
                        disabled={isGenerating}
                        onClick={() => setDuration(opt.value)}
                        className={`min-h-[44px] px-3 py-2 rounded-xl border text-xs font-semibold transition-colors whitespace-nowrap cursor-pointer ${
                          active
                            ? 'bg-purple-600 border-purple-400 text-white'
                            : 'bg-[#090812] border-white/10 text-slate-300 hover:border-purple-500/40'
                        }`}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Step 3: Topic / Context Input */}
            <div className="p-6 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-4">
              <div className="flex items-center justify-between">
                <label className="text-sm font-bold text-slate-100">
                  3. Topic or Gameplay Context
                </label>
                <span className="text-xs font-mono tabular-nums text-slate-400">
                  {topic.length}/600
                </span>
              </div>

              <textarea
                rows={4}
                value={topic}
                onChange={(e) => {
                  setTopic(e.target.value.slice(0, 600));
                  setActionError(null);
                }}
                disabled={isGenerating}
                placeholder={selectedToolObj.placeholder}
                className="w-full px-4 py-3 rounded-xl bg-[#090812] border border-white/10 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-purple-400"
              />

              {selectedTool === 'HASHTAGS' && (
                <div className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-300">
                    Intended Platform
                  </label>
                  <input
                    type="text"
                    value={platformHint}
                    onChange={(e) => setPlatformHint(e.target.value.slice(0, 60))}
                    disabled={isGenerating}
                    placeholder="e.g., YouTube Shorts, TikTok, Instagram Reels"
                    className="w-full px-3.5 py-2.5 rounded-xl bg-[#090812] border border-white/10 text-xs text-slate-100"
                  />
                </div>
              )}

              {selectedTool === 'THUMBNAIL_PROMPT' && (
                <div className="space-y-2">
                  <div className="text-xs font-semibold text-slate-300">
                    Requested Aspect Ratio (Prompt Text Only — Does Not Generate Images)
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {['16:9', '9:16', '1:1', '4:3'].map((ratio) => (
                      <button
                        key={ratio}
                        type="button"
                        disabled={isGenerating}
                        onClick={() => setAspectRatio(ratio)}
                        className={`px-3.5 py-1.5 rounded-lg border text-xs font-mono transition-colors ${
                          aspectRatio === ratio
                            ? 'bg-purple-600 border-purple-400 text-white'
                            : 'bg-[#090812] border-white/10 text-slate-300'
                        }`}
                      >
                        {ratio}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Step 4: Output Language */}
            <div className="p-6 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-3">
              <div>
                <div className="text-sm font-bold text-slate-100">4. Output Language</div>
                <p className="text-xs text-slate-400">
                  Changes generated AI text only. The application interface stays in English.
                </p>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {(
                  [
                    ['ENGLISH', 'English', 'Standard gaming creator English'],
                    [
                      'ROMAN_URDU',
                      'Roman Urdu',
                      'Urdu written in Latin letters (no Hindi script)',
                    ],
                  ] as const
                ).map(([val, title, desc]) => {
                  const active = outputLanguage === val;
                  return (
                    <button
                      key={val}
                      type="button"
                      disabled={isGenerating}
                      onClick={() => setOutputLanguage(val)}
                      className={`p-3.5 rounded-xl border text-left transition-colors cursor-pointer ${
                        active
                          ? 'bg-[#1b1433] border-purple-400'
                          : 'bg-[#090812] border-white/10 hover:border-purple-500/40'
                      }`}
                    >
                      <div className="text-sm font-bold text-slate-100">{title}</div>
                      <div className="text-xs text-slate-400 mt-0.5">{desc}</div>
                    </button>
                  );
                })}
              </div>
            </div>

            {actionError && (
              <div className="p-4 rounded-xl bg-[#261119] border border-red-500/40 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <p className="text-sm text-slate-200">{actionError}</p>
                {quota.remaining > 0 && (
                  <button
                    type="button"
                    onClick={handleRetry}
                    className="px-3.5 py-2 rounded-lg border border-purple-400/40 text-xs font-semibold text-slate-100 hover:bg-white/5 whitespace-nowrap shrink-0"
                  >
                    Retry
                  </button>
                )}
              </div>
            )}

            <button
              type="button"
              disabled={isGenerating || quota.remaining <= 0}
              onClick={() => void executeGenerate('generate')}
              className="w-full min-h-[52px] px-6 py-3.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-sm flex items-center justify-center gap-2.5 transition-colors disabled:opacity-50 cursor-pointer"
            >
              {isGenerating ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Generating with Gemini AI...</span>
                </>
              ) : quota.remaining <= 0 ? (
                <span>
                  Daily Limit Reached (Resets at {formatPktReset(quota.resetsAtIso)})
                </span>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Generate {selectedToolObj.shortTitle}</span>
                </>
              )}
            </button>
          </div>
        ) : activeNav === 'help' ? (
          /* GET HELP VIEW */
          <div className="max-w-3xl mx-auto space-y-6">
            <div>
              <h1 className="text-2xl font-extrabold text-slate-50">Get Help</h1>
              <p className="text-sm text-slate-400 mt-1">
                How to create gaming content with SN Creator and contact our support team.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-[#131024] border border-purple-500/35 space-y-4">
              <h2 className="text-lg font-bold text-slate-50">Direct WhatsApp Support</h2>
              <p className="text-sm text-slate-300 leading-relaxed">
                Need help signing in, configuring your account, or using one of the AI tools?
                Tap below to open WhatsApp with a prefilled support message.
              </p>
              <a
                href={WHATSAPP_HELP_URL}
                target="_blank"
                rel="noreferrer"
                className="inline-flex min-h-[48px] px-5 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm items-center gap-2 transition-colors whitespace-nowrap"
              >
                <MessageCircle className="w-4 h-4" />
                <span>Get Help on WhatsApp</span>
              </a>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[
                {
                  title: '1. Select Any Supported or Custom Game',
                  body: 'Pick PUBG Mobile, Free Fire, Last Asylum: Plague, Gangstar Mirage City, or choose Other Game to type any custom title.',
                },
                {
                  title: '2. Choose From Five Real AI Tools',
                  body: 'Generate video titles, platform hashtags, voice-over scripts (15s to 10m), copy-ready thumbnail text prompts, or video ideas.',
                },
                {
                  title: '3. English & Roman Urdu Output',
                  body: 'Generate directly in English or Roman Urdu (Latin alphabet), or translate active results on the Result screen.',
                },
                {
                  title: '4. 10 Daily Generations & Zero History',
                  body: 'Each account receives 10 successful generations per day (resetting at 12:00 AM Asia/Karachi). Copy outputs you want to keep — history is never saved.',
                },
              ].map((step) => (
                <div
                  key={step.title}
                  className="p-5 rounded-2xl bg-[#110f1d] border border-white/10 space-y-2"
                >
                  <h3 className="text-sm font-bold text-slate-100">{step.title}</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">{step.body}</p>
                </div>
              ))}
            </div>
          </div>
        ) : activeNav === 'settings' ? (
          /* SETTINGS VIEW */
          <div className="max-w-3xl mx-auto space-y-6">
            <h1 className="text-2xl font-extrabold text-slate-50">Settings</h1>

            <div className="p-6 rounded-2xl bg-[#110f1d] border border-purple-500/25 flex items-center justify-between gap-4">
              <div>
                <div className="text-xs text-purple-300">Google Account</div>
                <div className="text-lg font-bold text-slate-50 mt-0.5">{displayName}</div>
                <div className="text-xs text-slate-400">{currentUser.email}</div>
              </div>
              <button
                type="button"
                onClick={() => void handleSignOut()}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-red-500/40 hover:bg-red-950/40 text-xs font-semibold text-red-300 flex items-center gap-2 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <LogOut className="w-4 h-4" />
                <span>Sign Out</span>
              </button>
            </div>

            <div className="p-6 rounded-2xl bg-[#110f1d] border border-white/10 space-y-3">
              <div>
                <h2 className="text-base font-bold text-slate-100">
                  Generated-Content Language Preference
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  Sets the default language for generated AI outputs. App menus always stay in
                  English.
                </p>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {(
                  [
                    ['ENGLISH', 'English'],
                    ['ROMAN_URDU', 'Roman Urdu (Latin Script)'],
                  ] as const
                ).map(([code, label]) => (
                  <button
                    key={code}
                    type="button"
                    onClick={() => handlePreferredLanguageChange(code)}
                    className={`min-h-[44px] px-4 py-2.5 rounded-xl border text-xs font-semibold text-left transition-colors cursor-pointer ${
                      preferredLanguage === code
                        ? 'bg-[#1b1433] border-purple-400 text-slate-50'
                        : 'bg-[#090812] border-white/10 text-slate-300'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="rounded-2xl bg-[#110f1d] border border-white/10 divide-y divide-white/10">
              <button
                type="button"
                onClick={() => setLegalDialog('privacy')}
                className="w-full p-4 flex items-center justify-between text-left hover:bg-white/5 transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Shield className="w-4 h-4 text-purple-300" />
                  <span className="text-sm font-semibold text-slate-100">Privacy Policy</span>
                </div>
                <span className="text-xs text-slate-400">View</span>
              </button>

              <button
                type="button"
                onClick={() => setLegalDialog('terms')}
                className="w-full p-4 flex items-center justify-between text-left hover:bg-white/5 transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <FileText className="w-4 h-4 text-purple-300" />
                  <span className="text-sm font-semibold text-slate-100">Terms of Use</span>
                </div>
                <span className="text-xs text-slate-400">View</span>
              </button>

              <button
                type="button"
                onClick={() => setLegalDialog('contact')}
                className="w-full p-4 flex items-center justify-between text-left hover:bg-white/5 transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <MessageCircle className="w-4 h-4 text-purple-300" />
                  <div>
                    <div className="text-sm font-semibold text-slate-100">
                      Contact SN Creator
                    </div>
                    <div className="text-xs text-slate-400">
                      Configurable WhatsApp placeholder (UNCONFIGURED_CONTACT_NUMBER)
                    </div>
                  </div>
                </div>
                <span className="text-xs text-slate-400">Details</span>
              </button>

              <div className="p-4 flex items-center justify-between">
                <span className="text-sm font-semibold text-slate-100">App Version</span>
                <span className="text-xs font-mono tabular-nums text-slate-400">
                  1.0.0 (com.sncreator.app)
                </span>
              </div>
            </div>
          </div>
        ) : (
          /* ANDROID APK & GITHUB SETUP HUB */
          <div className="max-w-4xl mx-auto space-y-6">
            <div>
              <h1 className="text-2xl font-extrabold text-slate-50">
                Android Project & GitHub APK Workflow
              </h1>
              <p className="text-sm text-slate-400 mt-1">
                Complete Kotlin + Jetpack Compose source (`app/`), Cloudflare Workers + D1
                backend (`backend/`), and `.github/workflows/android-apk.yml` are ready in this
                repository.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-5 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-1">
                <div className="text-xs text-slate-400">Application ID</div>
                <div className="text-sm font-mono font-semibold text-purple-300">
                  com.sncreator.app
                </div>
                <div className="text-xs text-slate-400 pt-1">
                  Min SDK 26 · Target/Compile SDK 35
                </div>
              </div>

              <div className="p-5 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-1">
                <div className="text-xs text-slate-400">Provisioned Firebase Project</div>
                <div className="text-sm font-mono font-semibold text-slate-100 truncate">
                  {provisionedFirebaseMeta.projectId}
                </div>
                <div className="text-xs text-slate-400 pt-1">
                  Spark No-Billing Tier · Google Auth
                </div>
              </div>

              <div className="p-5 rounded-2xl bg-[#110f1d] border border-purple-500/25 space-y-1">
                <div className="text-xs text-slate-400">CI APK Artifact Path</div>
                <div className="text-sm font-mono font-semibold text-emerald-300 truncate">
                  SN-Creator-debug-apk
                </div>
                <div className="text-xs text-slate-400 pt-1">
                  app/build/outputs/apk/debug/app-debug.apk
                </div>
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-[#110f1d] border border-white/10 space-y-4">
              <h2 className="text-base font-bold text-slate-100">
                Quick Export & APK Download Steps (See README.md for Full Details)
              </h2>
              <ol className="list-decimal list-inside space-y-2.5 text-xs text-slate-300 leading-relaxed">
                <li>
                  Export/sync this project from Google AI Studio to a GitHub repository on the{' '}
                  <code className="text-purple-300">main</code> branch.
                </li>
                <li>
                  In Firebase Console (<code className="text-purple-300">{provisionedFirebaseMeta.projectId}</code>),
                  register Android app <code className="text-purple-300">com.sncreator.app</code>{' '}
                  with your debug keystore SHA-1 and copy <code className="text-purple-300">google-services.json</code>{' '}
                  into GitHub Secret <code className="text-purple-300">GOOGLE_SERVICES_JSON</code>.
                </li>
                <li>
                  Set <code className="text-purple-300">FIREBASE_WEB_CLIENT_ID</code> to{' '}
                  <code className="text-purple-300 break-all">
                    {provisionedFirebaseMeta.webClientId}
                  </code>
                  .
                </li>
                <li>
                  Deploy <code className="text-purple-300">backend/</code> to Cloudflare Workers
                  Free + D1 Free, run <code className="text-purple-300">npx wrangler secret put GEMINI_API_KEY</code>,
                  and set <code className="text-purple-300">SN_BACKEND_BASE_URL</code> in GitHub
                  Actions secrets/variables.
                </li>
                <li>
                  Open GitHub <strong>Actions → Android Debug APK CI → Run workflow</strong> and
                  download the <code className="text-purple-300">SN-Creator-debug-apk</code>{' '}
                  artifact to install on your Android phone.
                </li>
              </ol>
            </div>
          </div>
        )}
      </main>

      {/* Mobile Bottom Navigation Bar (4 primary sections matching Android app) */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#090812]/95 backdrop-blur-md border-t border-white/10 grid grid-cols-4 h-16">
        {(
          [
            ['home', 'Home', Gamepad2],
            ['create', 'Create', Sparkles],
            ['help', 'Get Help', HelpCircle],
            ['settings', 'Settings', SettingsIcon],
          ] as const
        ).map(([id, label, Icon]) => {
          const active = activeNav === id && !viewingResult;
          return (
            <button
              key={id}
              type="button"
              onClick={() => {
                setViewingResult(false);
                setActionError(null);
                setActiveNav(id);
              }}
              className={`flex flex-col items-center justify-center gap-1 min-h-[44px] ${
                active ? 'text-purple-300' : 'text-slate-400'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="text-[11px] font-medium whitespace-nowrap">{label}</span>
            </button>
          );
        })}
      </nav>

      {/* Legal / Contact Modals */}
      {legalDialog && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="max-w-lg w-full rounded-2xl bg-[#131122] border border-purple-500/35 p-6 space-y-4">
            <h3 className="text-lg font-bold text-slate-50">
              {legalDialog === 'privacy'
                ? 'Privacy Policy'
                : legalDialog === 'terms'
                  ? 'Terms of Use'
                  : 'Contact SN Creator'}
            </h3>
            <div className="max-h-80 overflow-y-auto text-xs text-slate-300 leading-relaxed space-y-2.5">
              {legalDialog === 'privacy' && (
                <>
                  <p>
                    SN Creator verifies your Google identity via Firebase Authentication and
                    stores only your verified UID and daily generation count (Asia/Karachi, 10
                    generations/day) in our backend quota table.
                  </p>
                  <p>
                    We never save a history of your prompts or generated titles, hashtags,
                    voice-over scripts, thumbnail prompts, or video ideas in cloud or local
                    storage. Full details are provided in PRIVACY_POLICY.md.
                  </p>
                </>
              )}
              {legalDialog === 'terms' && (
                <>
                  <p>
                    Authenticated Google sign-in is required. Each account receives up to 10
                    successful AI generations per Pakistan calendar day (Asia/Karachi);
                    regenerations and AI translations each count as 1 generation, while failed
                    requests never deduct quota.
                  </p>
                  <p>
                    Outputs are AI-generated drafts without guarantees of views or virality.
                    Full details are provided in TERMS_OF_USE.md.
                  </p>
                </>
              )}
              {legalDialog === 'contact' && (
                <p>
                  The dedicated Contact SN Creator WhatsApp number is not finalized yet and is
                  kept as an explicit configurable placeholder (
                  <code>SN_CONTACT_WHATSAPP_NUMBER = UNCONFIGURED_CONTACT_NUMBER</code>) rather
                  than inventing a phone number. For immediate app support, open the Get Help
                  section and tap Get Help on WhatsApp.
                </p>
              )}
            </div>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setLegalDialog(null)}
                className="px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-500 text-xs font-semibold text-white"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function heroImgImgOrFallback(src: string): string {
  return src;
}
