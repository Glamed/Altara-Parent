import { Badge, Tooltip } from '@mantine/core';
import type { ReportGroup, ReportStatus } from '../api/types';
import { titleCase } from '../format';

const STATUS_COLORS: Record<ReportStatus, string> = {
  PENDING: 'yellow',
  IN_PROGRESS: 'brand',
  ACCEPTED: 'green',
  REJECTED: 'gray',
  ABUSIVE: 'red',
  EXPIRED: 'dark',
};

export function StatusBadge({ status }: { status: ReportStatus }) {
  return <Badge color={STATUS_COLORS[status]} variant="light">{titleCase(status)}</Badge>;
}

export function GroupBadge({ group }: { group: ReportGroup }) {
  return (
    <Badge color={group === 'CHAT' ? 'discord' : 'orange'} variant="outline">
      {titleCase(group)}
    </Badge>
  );
}

export function PriorityBadge({ priority }: { priority?: number }) {
  if (priority === undefined) return null;
  const color = priority >= 45 ? 'red' : priority >= 38 ? 'orange' : 'gray';
  return (
    <Tooltip label="Queue priority — more reasons and fresher reports rank higher">
      <Badge color={color} variant="filled" miw={44}>{Math.round(priority)}</Badge>
    </Tooltip>
  );
}

export function OnlineBadge({ online, server }: { online?: boolean; server?: string | null }) {
  if (online === undefined) return null;
  return (
    <Badge color={online ? 'green' : 'gray'} variant="dot">
      {online ? server ?? 'Online' : 'Offline'}
    </Badge>
  );
}
