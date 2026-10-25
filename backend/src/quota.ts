import { D1DatabaseLike, QuotaStatus } from './types';

const PKT_OFFSET_MS = 5 * 60 * 60 * 1000; // Asia/Karachi is permanently UTC+05:00 (no DST)
const STALE_RESERVATION_TTL_MS = 60_000; // Auto-expire crashed in-flight locks after 60s

export function computePakistanDateWindow(nowMs: number = Date.now()): {
  pktDate: string;
  resetsAtIso: string;
} {
  const pktShifted = new Date(nowMs + PKT_OFFSET_MS);
  const year = pktShifted.getUTCFullYear();
  const month = pktShifted.getUTCMonth(); // 0-indexed
  const day = pktShifted.getUTCDate();

  const yyyy = String(year).padStart(4, '0');
  const mm = String(month + 1).padStart(2, '0');
  const dd = String(day).padStart(2, '0');
  const pktDate = `${yyyy}-${mm}-${dd}`;

  // Next midnight 00:00:00 in Asia/Karachi converted back to UTC Instant
  const nextMidnightUtcMs = Date.UTC(year, month, day + 1, 0, 0, 0, 0) - PKT_OFFSET_MS;
  const resetsAtIso = new Date(nextMidnightUtcMs).toISOString();

  return { pktDate, resetsAtIso };
}

export type ReservationOutcome =
  | { status: 'RESERVED'; quotaBeforeCommit: QuotaStatus }
  | { status: 'DUPLICATE'; quota: QuotaStatus }
  | { status: 'EXHAUSTED'; quota: QuotaStatus };

export class QuotaManager {
  private readonly db: D1DatabaseLike;
  private readonly dailyLimit: number;

  constructor(db: D1DatabaseLike, dailyLimit: number = 10) {
    this.db = db;
    this.dailyLimit = dailyLimit;
  }

  private async expireStaleInFlight(
    uid: string,
    pktDate: string,
    nowMs: number
  ): Promise<void> {
    const cutoff = nowMs - STALE_RESERVATION_TTL_MS;
    const expiredRes = await this.db
      .prepare(
        `UPDATE request_reservations
         SET status = 'ROLLED_BACK'
         WHERE uid = ? AND pkt_date = ? AND status = 'IN_FLIGHT' AND created_at < ?`
      )
      .bind(uid, pktDate, cutoff)
      .run();

    const expiredCount = expiredRes.meta?.changes ?? 0;
    if (expiredCount > 0) {
      await this.db
        .prepare(
          `UPDATE daily_quotas
           SET in_flight_count = MAX(0, in_flight_count - ?), updated_at = ?
           WHERE uid = ? AND pkt_date = ?`
        )
        .bind(expiredCount, nowMs, uid, pktDate)
        .run();
    }
  }

  async getQuotaStatus(uid: string, nowMs: number = Date.now()): Promise<QuotaStatus> {
    const { pktDate, resetsAtIso } = computePakistanDateWindow(nowMs);
    await this.expireStaleInFlight(uid, pktDate, nowMs);

    const row = await this.db
      .prepare(
        `SELECT used_count, in_flight_count
         FROM daily_quotas
         WHERE uid = ? AND pkt_date = ?`
      )
      .bind(uid, pktDate)
      .first<{ used_count: number; in_flight_count: number }>();

    const used = Math.min(this.dailyLimit, Math.max(0, Number(row?.used_count ?? 0)));
    const remaining = Math.max(0, this.dailyLimit - used);

    return {
      limit: this.dailyLimit,
      used,
      remaining,
      pktDate,
      resetsAtIso,
      timezone: 'Asia/Karachi',
    };
  }

  /**
   * Atomically reserves 1 quota unit prior to calling Gemini.
   * Prevents simultaneous duplicate taps from bypassing the 10/day limit or double-charging.
   */
  async reserveSlot(
    uid: string,
    requestId: string,
    nowMs: number = Date.now()
  ): Promise<ReservationOutcome> {
    const { pktDate } = computePakistanDateWindow(nowMs);
    await this.expireStaleInFlight(uid, pktDate, nowMs);

    // 1. Check idempotency record for duplicate tap / replay
    const existingReq = await this.db
      .prepare(
        `SELECT status FROM request_reservations WHERE request_id = ?`
      )
      .bind(requestId)
      .first<{ status: string }>();

    if (
      existingReq &&
      (existingReq.status === 'IN_FLIGHT' || existingReq.status === 'COMPLETED')
    ) {
      const currentQuota = await this.getQuotaStatus(uid, nowMs);
      return { status: 'DUPLICATE', quota: currentQuota };
    }

    // 2. Ensure daily row exists
    await this.db
      .prepare(
        `INSERT OR IGNORE INTO daily_quotas (uid, pkt_date, used_count, in_flight_count, updated_at)
         VALUES (?, ?, 0, 0, ?)`
      )
      .bind(uid, pktDate, nowMs)
      .run();

    // 3. Atomic conditional increment: succeeds ONLY if (used_count + in_flight_count) < dailyLimit
    const reserveUpdate = await this.db
      .prepare(
        `UPDATE daily_quotas
         SET in_flight_count = in_flight_count + 1, updated_at = ?
         WHERE uid = ? AND pkt_date = ? AND (used_count + in_flight_count) < ?`
      )
      .bind(nowMs, uid, pktDate, this.dailyLimit)
      .run();

    if ((reserveUpdate.meta?.changes ?? 0) === 0) {
      const currentQuota = await this.getQuotaStatus(uid, nowMs);
      return { status: 'EXHAUSTED', quota: currentQuota };
    }

    // 4. Record reservation
    if (existingReq) {
      await this.db
        .prepare(
          `UPDATE request_reservations
           SET status = 'IN_FLIGHT', pkt_date = ?, created_at = ?
           WHERE request_id = ? AND uid = ?`
        )
        .bind(pktDate, nowMs, requestId, uid)
        .run();
    } else {
      await this.db
        .prepare(
          `INSERT INTO request_reservations (request_id, uid, pkt_date, status, created_at)
           VALUES (?, ?, ?, 'IN_FLIGHT', ?)`
        )
        .bind(requestId, uid, pktDate, nowMs)
        .run();
    }

    const quotaBeforeCommit = await this.getQuotaStatus(uid, nowMs);
    return { status: 'RESERVED', quotaBeforeCommit };
  }

  /**
   * Commits a reserved slot after Gemini returns valid generated content.
   */
  async commitReservation(
    uid: string,
    requestId: string,
    nowMs: number = Date.now()
  ): Promise<QuotaStatus> {
    const resRow = await this.db
      .prepare(
        `SELECT pkt_date, status FROM request_reservations WHERE request_id = ? AND uid = ?`
      )
      .bind(requestId, uid)
      .first<{ pkt_date: string; status: string }>();

    if (resRow && resRow.status === 'IN_FLIGHT') {
      const markDone = await this.db
        .prepare(
          `UPDATE request_reservations
           SET status = 'COMPLETED'
           WHERE request_id = ? AND uid = ? AND status = 'IN_FLIGHT'`
        )
        .bind(requestId, uid)
        .run();

      if ((markDone.meta?.changes ?? 0) > 0) {
        await this.db
          .prepare(
            `UPDATE daily_quotas
             SET in_flight_count = MAX(0, in_flight_count - 1),
                 used_count = MIN(?, used_count + 1),
                 updated_at = ?
             WHERE uid = ? AND pkt_date = ?`
          )
          .bind(this.dailyLimit, nowMs, uid, resRow.pkt_date)
          .run();
      }
    }

    return this.getQuotaStatus(uid, nowMs);
  }

  /**
   * Rolls back a reserved slot whenever Gemini fails, times out, or hits an upstream rate limit.
   * Guarantees failed requests NEVER consume user quota.
   */
  async rollbackReservation(
    uid: string,
    requestId: string,
    nowMs: number = Date.now()
  ): Promise<QuotaStatus> {
    const resRow = await this.db
      .prepare(
        `SELECT pkt_date, status FROM request_reservations WHERE request_id = ? AND uid = ?`
      )
      .bind(requestId, uid)
      .first<{ pkt_date: string; status: string }>();

    if (resRow && resRow.status === 'IN_FLIGHT') {
      const markRolledBack = await this.db
        .prepare(
          `UPDATE request_reservations
           SET status = 'ROLLED_BACK'
           WHERE request_id = ? AND uid = ? AND status = 'IN_FLIGHT'`
        )
        .bind(requestId, uid)
        .run();

      if ((markRolledBack.meta?.changes ?? 0) > 0) {
        await this.db
          .prepare(
            `UPDATE daily_quotas
             SET in_flight_count = MAX(0, in_flight_count - 1),
                 updated_at = ?
             WHERE uid = ? AND pkt_date = ?`
          )
          .bind(nowMs, uid, resRow.pkt_date)
          .run();
      }
    }

    return this.getQuotaStatus(uid, nowMs);
  }
}
