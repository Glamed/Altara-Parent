export type ReportStatus = 'PENDING' | 'IN_PROGRESS' | 'ACCEPTED' | 'REJECTED' | 'ABUSIVE' | 'EXPIRED';
export type ReportGroup = 'CHAT' | 'GAMEPLAY';

export const TERMINAL_STATUSES: ReportStatus[] = ['ACCEPTED', 'REJECTED', 'ABUSIVE', 'EXPIRED'];
export const ALL_STATUSES: ReportStatus[] = ['PENDING', 'IN_PROGRESS', ...TERMINAL_STATUSES];

export interface ReportReason {
  reporterUuid: string | null;
  server: string | null;
  category: string;
  reportedAt: number;
}

export interface ReportMessage {
  senderUuid: string;
  sentAt: number;
  message: string;
  recipients: string[];
  reportedBy: string[];
  riskLevel: number;
}

export interface Report {
  id: string;
  suspectUuid: string;
  group: ReportGroup;
  reasons: ReportReason[];
  messages: ReportMessage[];
  status: ReportStatus;
  handler?: string | null;
  handlerServer?: string | null;
  statusReason?: string | null;
  statusTime: number;
  createdAt: number;
  /** Added by /queue and search. */
  priority?: number;
  /** Added by /queue only. */
  suspectOnline?: boolean;
  suspectServer?: string | null;
  handlerOnline?: boolean;
}

export interface Queue {
  pending: Report[];
  inProgress: Report[];
}

export interface ReportCategory {
  name: string;
  group: ReportGroup;
  displayName: string;
  description: string;
}

export interface RejectionType {
  name: string;
  abusive: boolean;
  displayName: string;
  description: string;
}

export type EngageOutcome = 'ENGAGED' | 'NOT_FOUND' | 'CLOSED' | 'STAFF_OFFLINE' | 'TAKEN' | 'BUSY' | 'EMPTY';

export interface EngageResult {
  outcome: EngageOutcome;
  report?: Report;
}

export interface PlayerRef {
  uuid: string;
  name: string;
}

export type ResolveStatus = Extract<ReportStatus, 'ACCEPTED' | 'REJECTED' | 'ABUSIVE' | 'EXPIRED'>;
