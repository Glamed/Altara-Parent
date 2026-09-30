import { Button, Group, Menu, Text } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import {
  IconArrowBackUp, IconCheck, IconChevronDown, IconDeviceGamepad2, IconHandGrab, IconX,
} from '@tabler/icons-react';
import { useClaim, useRejectionTypes, useRelease, useResolve } from '../api/reports';
import type { Report, ResolveStatus } from '../api/types';
import { TERMINAL_STATUSES } from '../api/types';
import { useSignedInStaff } from '../staff/StaffContext';
import { PlayerName } from './PlayerName';
import { useEngageFlow } from './useEngageFlow';

/** The most-reported category — what an accepted report is recorded as (like the in-game menu). */
function dominantCategory(report: Report): string | null {
  const counts = new Map<string, number>();
  for (const reason of report.reasons) counts.set(reason.category, (counts.get(reason.category) ?? 0) + 1);
  let best: string | null = null;
  for (const [category, count] of counts) if (!best || count > counts.get(best)!) best = category;
  return best;
}

export function ReportActions({ report }: { report: Report }) {
  const staff = useSignedInStaff();
  const { engage, pending: engaging } = useEngageFlow();
  const claim = useClaim();
  const release = useRelease();
  const resolve = useResolve();
  const { data: rejectionTypes = [] } = useRejectionTypes();

  const mine = report.status === 'IN_PROGRESS' && report.handler === staff.uuid;
  const fail = (error: Error) => notifications.show({ color: 'red', title: 'Request failed', message: error.message });

  if (TERMINAL_STATUSES.includes(report.status)) return null;

  if (report.status === 'IN_PROGRESS' && !mine) {
    return (
      <Group gap={6}>
        <Text size="sm" c="dimmed">Being handled by</Text>
        <PlayerName uuid={report.handler} />
      </Group>
    );
  }

  const close = (status: ResolveStatus, reasonDetail: string | null, label: string) =>
    modals.openConfirmModal({
      title: `${label}?`,
      children: <Text size="sm">This closes the report and notifies whoever is handling it in-game.</Text>,
      labels: { confirm: label, cancel: 'Cancel' },
      confirmProps: { color: status === 'ACCEPTED' ? 'green' : 'red' },
      onConfirm: () =>
        resolve.mutate(
          { reportId: report.id, staffUuid: staff.uuid, status, reasonDetail },
          {
            onSuccess: () => notifications.show({ color: 'green', title: 'Report closed', message: label }),
            onError: fail,
          },
        ),
    });

  if (!mine) {
    return (
      <Group>
        <Button leftSection={<IconDeviceGamepad2 size={18} />} loading={engaging} onClick={() => engage(report.id)}>
          Handle in-game
        </Button>
        <Button
          variant="light"
          leftSection={<IconHandGrab size={18} />}
          loading={claim.isPending}
          onClick={() => claim.mutate({ reportId: report.id, staffUuid: staff.uuid }, { onError: fail })}
        >
          Claim on web
        </Button>
      </Group>
    );
  }

  return (
    <Group>
      <Button color="green" leftSection={<IconCheck size={18} />} onClick={() => close('ACCEPTED', dominantCategory(report), 'Accept report')}>
        Accept
      </Button>

      <Menu position="bottom-start" withinPortal>
        <Menu.Target>
          <Button color="red" variant="light" leftSection={<IconX size={18} />} rightSection={<IconChevronDown size={16} />}>
            Reject
          </Button>
        </Menu.Target>
        <Menu.Dropdown>
          {rejectionTypes.map((type) => (
            <Menu.Item
              key={type.name}
              color={type.abusive ? 'red' : undefined}
              onClick={() => close(type.abusive ? 'ABUSIVE' : 'REJECTED', type.name, type.displayName)}
            >
              <Text size="sm" fw={500}>{type.displayName}</Text>
              <Text size="xs" c="dimmed">{type.description}</Text>
            </Menu.Item>
          ))}
        </Menu.Dropdown>
      </Menu>

      <Button variant="default" leftSection={<IconDeviceGamepad2 size={18} />} loading={engaging} onClick={() => engage(report.id)}>
        Go in-game
      </Button>

      <Button
        variant="subtle"
        color="gray"
        leftSection={<IconArrowBackUp size={18} />}
        loading={release.isPending}
        onClick={() => release.mutate(report.id, { onError: fail })}
      >
        Release
      </Button>
    </Group>
  );
}
