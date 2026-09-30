import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError, get, post } from './client';
import type {
  EngageResult, PlayerRef, Queue, RejectionType, Report, ReportCategory, ReportGroup, ReportStatus, ResolveStatus,
} from './types';

const QUEUE_REFRESH_MS = 5_000;
export const HISTORY_PAGE_SIZE = 50;

export const keys = {
  queue: ['reports', 'queue'] as const,
  report: (id: string) => ['reports', 'one', id] as const,
  handling: (staff: string) => ['reports', 'handling', staff] as const,
  history: (filters: HistoryFilters) => ['reports', 'history', filters] as const,
  player: (uuid: string) => ['player', uuid] as const,
};

// ── Queries ─────────────────────────────────────────────────────────────────

export function useQueue() {
  return useQuery({
    queryKey: keys.queue,
    queryFn: () => get<Queue>('/api/report/queue'),
    refetchInterval: QUEUE_REFRESH_MS,
  });
}

export function useReport(id: string | undefined) {
  return useQuery({
    queryKey: keys.report(id ?? ''),
    queryFn: () => get<Report>(`/api/report/${id}`),
    enabled: !!id,
    refetchInterval: QUEUE_REFRESH_MS,
  });
}

/** The report this staff member is holding right now (null when none). */
export function useHandling(staffUuid: string | undefined) {
  return useQuery({
    queryKey: keys.handling(staffUuid ?? ''),
    queryFn: () => get<Report | null>(`/api/report/handler/${staffUuid}`),
    enabled: !!staffUuid,
    refetchInterval: 15_000,
  });
}

export interface HistoryFilters {
  statuses: ReportStatus[];
  group: ReportGroup | null;
  suspect: string | null;
  handler: string | null;
}

/** @param enabled false while a filter is still being resolved (e.g. a suspect name → UUID). */
export function useHistory(filters: HistoryFilters, enabled = true) {
  return useInfiniteQuery({
    queryKey: keys.history(filters),
    enabled,
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam }) => {
      const params = new URLSearchParams({ limit: String(HISTORY_PAGE_SIZE) });
      if (filters.statuses.length) params.set('status', filters.statuses.join(','));
      if (filters.group) params.set('group', filters.group);
      if (filters.suspect) params.set('suspect', filters.suspect);
      if (filters.handler) params.set('handler', filters.handler);
      if (pageParam !== undefined) params.set('before', String(pageParam));
      return get<Report[]>(`/api/report?${params}`);
    },
    // The API pages on a createdAt cursor: pass the last report's createdAt to get older ones.
    getNextPageParam: (page) => (page.length < HISTORY_PAGE_SIZE ? undefined : page[page.length - 1].createdAt),
  });
}

export function useCategories() {
  return useQuery({
    queryKey: ['reports', 'categories'],
    queryFn: () => get<ReportCategory[]>('/api/report/categories'),
    staleTime: Infinity,
  });
}

export function useRejectionTypes() {
  return useQuery({
    queryKey: ['reports', 'rejection-types'],
    queryFn: () => get<RejectionType[]>('/api/report/rejection-types'),
    staleTime: Infinity,
  });
}

/** Resolves a UUID to its current name (cached for the session). */
export function usePlayer(uuid: string | null | undefined) {
  return useQuery({
    queryKey: keys.player(uuid ?? ''),
    queryFn: () => get<PlayerRef>(`/api/uuid/${uuid}`),
    enabled: !!uuid,
    staleTime: Infinity,
    retry: false,
  });
}

export const lookupPlayerByName = (name: string) => get<PlayerRef>(`/api/uuid/name/${encodeURIComponent(name)}`);

// ── Mutations ───────────────────────────────────────────────────────────────

/**
 * Engage answers 200, 404 and 409 with the same {outcome, report} body, so those are
 * results rather than errors — the caller decides what BUSY / TAKEN / STAFF_OFFLINE mean.
 */
async function engageRequest(path: string, body: unknown): Promise<EngageResult> {
  try {
    return await post<EngageResult>(path, body);
  } catch (error) {
    if (error instanceof ApiError && error.body && typeof error.body === 'object' && 'outcome' in error.body) {
      return error.body as EngageResult;
    }
    throw error;
  }
}

function useInvalidateReports() {
  const client = useQueryClient();
  return () => client.invalidateQueries({ queryKey: ['reports'] });
}

export interface EngageVariables {
  staffUuid: string;
  /** Omit for "next report in the queue". */
  reportId?: string;
  force?: boolean;
}

export function useEngage() {
  const invalidate = useInvalidateReports();
  return useMutation({
    mutationFn: ({ staffUuid, reportId, force }: EngageVariables) =>
      reportId
        ? engageRequest(`/api/report/${reportId}/engage`, { staffUuid, force })
        : engageRequest('/api/report/engage-next', { staffUuid, force }),
    onSettled: invalidate,
  });
}

/** Claim on the website only — nobody gets pulled in-game (e.g. a chat report handled from here). */
export function useClaim() {
  const invalidate = useInvalidateReports();
  return useMutation({
    mutationFn: ({ reportId, staffUuid }: { reportId: string; staffUuid: string }) =>
      post<Report>(`/api/report/${reportId}/claim`, { staffUuid, server: 'web', source: 'web' }),
    onSettled: invalidate,
  });
}

export function useRelease() {
  const invalidate = useInvalidateReports();
  return useMutation({
    mutationFn: (reportId: string) => post<Report>(`/api/report/${reportId}/release`, { source: 'web' }),
    onSettled: invalidate,
  });
}

export interface ResolveVariables {
  reportId: string;
  staffUuid: string;
  status: ResolveStatus;
  reasonDetail?: string | null;
}

export function useResolve() {
  const invalidate = useInvalidateReports();
  return useMutation({
    mutationFn: ({ reportId, ...body }: ResolveVariables) =>
      post<Report>(`/api/report/${reportId}/resolve`, { ...body, source: 'web' }),
    onSettled: invalidate,
  });
}

export const heartbeat = (reportId: string, staffUuid: string) =>
  post(`/api/report/${reportId}/heartbeat`, { staffUuid });
