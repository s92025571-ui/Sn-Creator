import 'dotenv/config';
import express from 'express';
import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';
import { fileURLToPath } from 'node:url';
import { handleBackendRequest } from './backend/src/index.ts';
import type {
  BackendEnv,
  D1DatabaseLike,
  D1PreparedStatementLike,
} from './backend/src/types.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function createNodeSqliteQuotaDb(): D1DatabaseLike {
  const sqlite = new DatabaseSync(':memory:');
  const schemaPath = path.resolve(__dirname, 'backend/schema.sql');
  const schemaSql = fs.readFileSync(schemaPath, 'utf-8');
  sqlite.exec(schemaSql);

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

function resolveProvisionedFirebaseProjectId(): string {
  if (process.env.FIREBASE_PROJECT_ID?.trim()) {
    return process.env.FIREBASE_PROJECT_ID.trim();
  }
  try {
    const cfgPath = path.resolve(__dirname, 'firebase-applet-config.json');
    if (fs.existsSync(cfgPath)) {
      const parsed = JSON.parse(fs.readFileSync(cfgPath, 'utf-8')) as {
        projectId?: string;
      };
      if (parsed.projectId?.trim()) {
        return parsed.projectId.trim();
      }
    }
  } catch {
    // Ignore parse error and let handler surface misconfiguration cleanly
  }
  return '';
}

function startServer() {
  const app = express();
  const port = 3000;
  const quotaDb = createNodeSqliteQuotaDb();

  // Mount raw text body strictly for /api/v1/* and /v1/* so the Cloudflare Worker handler runs unmodified
  app.use(['/api/v1', '/v1'], express.text({ type: '*/*', limit: '24kb' }));

  app.all(['/api/v1/*', '/v1/*'], async (req, res) => {
    try {
      const protocol = req.headers['x-forwarded-proto'] || req.protocol || 'http';
      const host = req.headers.host || `localhost:${port}`;
      const fullUrl = `${protocol}://${host}${req.originalUrl}`;

      const headers = new Headers();
      for (const [key, val] of Object.entries(req.headers)) {
        if (typeof val === 'string') {
          headers.set(key, val);
        } else if (Array.isArray(val)) {
          headers.set(key, val.join(', '));
        }
      }

      const init: RequestInit = {
        method: req.method,
        headers,
      };
      if (req.method !== 'GET' && req.method !== 'HEAD' && typeof req.body === 'string') {
        init.body = req.body;
      }

      const env: BackendEnv = {
        QUOTA_DB: quotaDb,
        GEMINI_API_KEY: process.env.GEMINI_API_KEY,
        FIREBASE_PROJECT_ID: resolveProvisionedFirebaseProjectId(),
        GEMINI_MODEL: process.env.GEMINI_MODEL || 'gemini-3.8-flash',
        DAILY_QUOTA_LIMIT: process.env.DAILY_QUOTA_LIMIT || '10',
      };

      const workerResponse = await handleBackendRequest(
        new Request(fullUrl, init),
        env
      );

      res.status(workerResponse.status);
      workerResponse.headers.forEach((value, key) => {
        res.setHeader(key, value);
      });
      const responseText = await workerResponse.text();
      res.send(responseText);
    } catch {
      res.status(500).json({
        errorCode: 'UPSTREAM_ERROR',
        message: 'Unexpected server error while processing request.',
      });
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    // Initialize Vite asynchronously AFTER binding port 3000 so the health probe succeeds immediately
    const vitePromise = import('vite').then(({ createServer: createViteServer }) =>
      createViteServer({
        server: { middlewareMode: true },
        appType: 'spa',
      })
    );

    app.use(async (req, res, next) => {
      try {
        const vite = await vitePromise;
        vite.middlewares(req, res, next);
      } catch (err) {
        next(err);
      }
    });
  } else {
    const distPath = path.join(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  // Bind port 3000 immediately on startup so AI Studio's control plane detects the server right away
  app.listen(port, '0.0.0.0', () => {
    console.log(`SN Creator server listening on http://0.0.0.0:${port}`);
  });
}

startServer();
