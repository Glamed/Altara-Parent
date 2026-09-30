import type { ReactNode } from 'react';
import {
  ActionIcon, Alert, Anchor, Badge, Button, Card, Center, Group, Loader, Stack, Table, Text, Title, Tooltip,
} from '@mantine/core';
import { IconAlertCircle, IconDeviceGamepad2, IconInbox, IconPlayerTrackNext } from '@tabler/icons-react';
import { Link, useNavigate } from 'react-router-dom';
import { useHandling, useQueue } from '../api/reports';
import type { Report } from '../api/types';
import { GroupBadge, OnlineBadge, PriorityBadge } from '../components/Badges';
import { CategoryBadges } from '../components/CategoryBadges';
import { PlayerName } from '../components/PlayerName';
import { useEngageFlow } from '../components/useEngageFlow';
import { timeAgo } from '../format';
import { useSignedInStaff } from '../staff/StaffContext';

export function QueuePage() {
  const staff = useSignedInStaff();
  const { data: queue, isLoading, error } = useQueue();
  const { data: handling } = useHandling(staff.uuid);
  const { engage, pending: engaging } = useEngageFlow();

  return (
    <Stack>
      <Group justify="space-between">
        <div>
          <Title order={2}>Queue</Title>
          <Text c="dimmed" size="sm">Refreshes every few seconds. Highest priority first.</Text>
        </div>
        <Button
          size="md"
          leftSection={<IconPlayerTrackNext size={20} />}
          loading={engaging}
          onClick={() => engage()}
          disabled={!queue?.pending.length}
        >
          Handle next report
        </Button>
      </Group>

      {handling && <CurrentReport report={handling} />}

      {error && (
        <Alert color="red" icon={<IconAlertCircle />} title="Couldn't load the queue">
          {error.message}
        </Alert>
      )}

      {isLoading ? (
        <Center py="xl"><Loader /></Center>
      ) : (
        queue && (
          <>
            <Section title="Pending" count={queue.pending.length}>
              {queue.pending.length === 0 ? (
                <Empty text="No reports waiting." />
              ) : (
                <PendingTable reports={queue.pending} onEngage={engage} engaging={engaging} />
              )}
            </Section>

            <Section title="In progress" count={queue.inProgress.length}>
              {queue.inProgress.length === 0 ? (
                <Empty text="Nobody is handling a report right now." />
              ) : (
                <InProgressTable reports={queue.inProgress} />
              )}
            </Section>
          </>
        )
      )}
    </Stack>
  );
}

function CurrentReport({ report }: { report: Report }) {
  return (
    <Alert color="brand" variant="light" icon={<IconDeviceGamepad2 />} title="You're handling a report">
      <Group justify="space-between">
        <Group gap={6}>
          <Text size="sm">Report on</Text>
          <PlayerName uuid={report.suspectUuid} />
        </Group>
        <Anchor component={Link} to={`/reports/${report.id}`} size="sm">Open report</Anchor>
      </Group>
    </Alert>
  );
}

function Section({ title, count, children }: { title: string; count: number; children: ReactNode }) {
  return (
    <Card withBorder padding={0}>
      <Group px="md" py="sm" gap="xs">
        <Title order={4}>{title}</Title>
        <Badge variant="light" color="gray">{count}</Badge>
      </Group>
      {children}
    </Card>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <Center py="xl">
      <Stack gap={4} align="center">
        <IconInbox size={32} color="var(--mantine-color-dimmed)" />
        <Text c="dimmed" size="sm">{text}</Text>
      </Stack>
    </Center>
  );
}

function PendingTable({ reports, onEngage, engaging }: {
  reports: Report[];
  onEngage: (id: string) => void;
  engaging: boolean;
}) {
  const navigate = useNavigate();
  return (
    <Table.ScrollContainer minWidth={760}>
      <Table highlightOnHover verticalSpacing="sm">
        <Table.Thead>
          <Table.Tr>
            <Table.Th w={70}>Priority</Table.Th>
            <Table.Th>Suspect</Table.Th>
            <Table.Th>Type</Table.Th>
            <Table.Th>Reasons</Table.Th>
            <Table.Th>Suspect is</Table.Th>
            <Table.Th>Opened</Table.Th>
            <Table.Th w={60} />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {reports.map((report) => (
            <Table.Tr key={report.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/reports/${report.id}`)}>
              <Table.Td><PriorityBadge priority={report.priority} /></Table.Td>
              <Table.Td><PlayerName uuid={report.suspectUuid} /></Table.Td>
              <Table.Td><GroupBadge group={report.group} /></Table.Td>
              <Table.Td><CategoryBadges reasons={report.reasons} /></Table.Td>
              <Table.Td><OnlineBadge online={report.suspectOnline} server={report.suspectServer} /></Table.Td>
              <Table.Td><Text size="sm" c="dimmed">{timeAgo(report.createdAt)}</Text></Table.Td>
              <Table.Td>
                <Tooltip label="Handle in-game">
                  <ActionIcon
                    variant="light"
                    loading={engaging}
                    onClick={(event) => { event.stopPropagation(); onEngage(report.id); }}
                  >
                    <IconDeviceGamepad2 size={18} />
                  </ActionIcon>
                </Tooltip>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

function InProgressTable({ reports }: { reports: Report[] }) {
  const navigate = useNavigate();
  return (
    <Table.ScrollContainer minWidth={640}>
      <Table highlightOnHover verticalSpacing="sm">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Suspect</Table.Th>
            <Table.Th>Type</Table.Th>
            <Table.Th>Handler</Table.Th>
            <Table.Th>Handler is</Table.Th>
            <Table.Th>Claimed</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {reports.map((report) => (
            <Table.Tr key={report.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/reports/${report.id}`)}>
              <Table.Td><PlayerName uuid={report.suspectUuid} /></Table.Td>
              <Table.Td><GroupBadge group={report.group} /></Table.Td>
              <Table.Td><PlayerName uuid={report.handler} /></Table.Td>
              <Table.Td>
                <OnlineBadge online={report.handlerOnline} server={report.handlerOnline ? 'In-game' : 'Web only'} />
              </Table.Td>
              <Table.Td><Text size="sm" c="dimmed">{timeAgo(report.statusTime)}</Text></Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
