import {
  Alert, Anchor, Card, Center, Group, Loader, SimpleGrid, Stack, Table, Text, Title,
} from '@mantine/core';
import { IconAlertCircle, IconArrowLeft } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useCategories, useReport } from '../api/reports';
import { GroupBadge, PriorityBadge, StatusBadge } from '../components/Badges';
import { ChatLog } from '../components/ChatLog';
import { PlayerName } from '../components/PlayerName';
import { ReportActions } from '../components/ReportActions';
import { dateTime, shortId, timeAgo, titleCase } from '../format';

export function ReportPage() {
  const { id } = useParams<{ id: string }>();
  const { data: report, isLoading, error } = useReport(id);
  const { data: categories = [] } = useCategories();
  const categoryName = (name: string) => categories.find((c) => c.name === name)?.displayName ?? titleCase(name);

  if (isLoading) return <Center py="xl"><Loader /></Center>;
  if (error || !report) {
    return (
      <Alert color="red" icon={<IconAlertCircle />} title="Couldn't load this report">
        {error?.message ?? 'Not found'}
      </Alert>
    );
  }

  return (
    <Stack>
      <Anchor component={Link} to="/" size="sm">
        <Group gap={4}><IconArrowLeft size={14} /> Back to queue</Group>
      </Anchor>

      <Card withBorder>
        <Stack>
          <Group justify="space-between" align="flex-start">
            <Stack gap="xs">
              <Group gap="sm">
                <PlayerName uuid={report.suspectUuid} size="lg" />
                <StatusBadge status={report.status} />
                <GroupBadge group={report.group} />
                <PriorityBadge priority={report.priority} />
              </Group>
              <Text size="xs" c="dimmed" ff="monospace">Report {shortId(report.id)}</Text>
            </Stack>
          </Group>
          <ReportActions report={report} />
        </Stack>
      </Card>

      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Card withBorder>
          <Title order={4} mb="sm">Reasons ({report.reasons.length})</Title>
          <Table verticalSpacing="xs">
            <Table.Tbody>
              {report.reasons.map((reason) => (
                <Table.Tr key={`${reason.reporterUuid}-${reason.reportedAt}`}>
                  <Table.Td><PlayerName uuid={reason.reporterUuid} fallback="Automated" /></Table.Td>
                  <Table.Td><Text size="sm">{categoryName(reason.category)}</Text></Table.Td>
                  <Table.Td><Text size="sm" c="dimmed">{reason.server ?? '—'}</Text></Table.Td>
                  <Table.Td><Text size="sm" c="dimmed">{timeAgo(reason.reportedAt)}</Text></Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>

        <Card withBorder>
          <Title order={4} mb="sm">Details</Title>
          <Stack gap="xs">
            <Detail label="Opened">{dateTime(report.createdAt)}</Detail>
            <Detail label="Last change">{dateTime(report.statusTime)}</Detail>
            <Detail label="Handler">
              {report.handler ? <PlayerName uuid={report.handler} /> : '—'}
            </Detail>
            <Detail label="Handler server">{report.handlerServer ?? '—'}</Detail>
            <Detail label="Outcome">{report.statusReason ? titleCase(report.statusReason) : '—'}</Detail>
          </Stack>
        </Card>
      </SimpleGrid>

      {report.group === 'CHAT' && (
        <Card withBorder>
          <Title order={4} mb="sm">Chat ({report.messages.length})</Title>
          <ChatLog messages={report.messages} suspectUuid={report.suspectUuid} />
        </Card>
      )}
    </Stack>
  );
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Group justify="space-between" wrap="nowrap">
      <Text size="sm" c="dimmed">{label}</Text>
      {typeof children === 'string' ? <Text size="sm">{children}</Text> : children}
    </Group>
  );
}
