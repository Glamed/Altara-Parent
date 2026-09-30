import {
  Alert, Button, Card, Center, Group, Loader, MultiSelect, Select, Stack, Table, Text, TextInput, Title,
} from '@mantine/core';
import { IconAlertCircle, IconSearch } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { lookupPlayerByName, useHistory, type HistoryFilters } from '../api/reports';
import { ALL_STATUSES, type ReportGroup, type ReportStatus } from '../api/types';
import { GroupBadge, StatusBadge } from '../components/Badges';
import { CategoryBadges } from '../components/CategoryBadges';
import { PlayerName } from '../components/PlayerName';
import { dateTime, titleCase } from '../format';

/** Report history/search. `?suspect=<name>` pre-fills the suspect filter (used by the command palette). */
export function HistoryPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();

  const [statuses, setStatuses] = useState<ReportStatus[]>([]);
  const [group, setGroup] = useState<ReportGroup | null>(null);
  const [suspectName, setSuspectName] = useState(params.get('suspect') ?? '');
  const [suspectUuid, setSuspectUuid] = useState<string | null>(null);
  const [suspectError, setSuspectError] = useState<string | null>(null);

  // The URL is the source of truth for the suspect filter, so the command palette can link here.
  const suspectParam = params.get('suspect') ?? '';
  useEffect(() => {
    setSuspectName(suspectParam);
    setSuspectError(null);
    if (!suspectParam) {
      setSuspectUuid(null);
      return;
    }

    let cancelled = false;
    lookupPlayerByName(suspectParam)
      .then((player) => { if (!cancelled) setSuspectUuid(player.uuid); })
      .catch(() => {
        if (cancelled) return;
        setSuspectUuid(null);
        setSuspectError('No player with that name');
      });
    return () => { cancelled = true; };
  }, [suspectParam]);

  const submitSuspect = () => setParams(suspectName.trim() ? { suspect: suspectName.trim() } : {});

  const filters: HistoryFilters = { statuses, group, suspect: suspectUuid, handler: null };
  const history = useHistory(filters, !suspectParam || !!suspectUuid);
  const reports = history.data?.pages.flat() ?? [];

  return (
    <Stack>
      <div>
        <Title order={2}>History</Title>
        <Text c="dimmed" size="sm">Every report, newest first.</Text>
      </div>

      <Card withBorder>
        <Group align="flex-end" grow>
          <MultiSelect
            label="Status"
            placeholder="Any"
            data={ALL_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))}
            value={statuses}
            onChange={(value) => setStatuses(value as ReportStatus[])}
            clearable
          />
          <Select
            label="Type"
            placeholder="Any"
            data={[{ value: 'CHAT', label: 'Chat' }, { value: 'GAMEPLAY', label: 'Gameplay' }]}
            value={group}
            onChange={(value) => setGroup(value as ReportGroup | null)}
            clearable
          />
          <form onSubmit={(event) => { event.preventDefault(); submitSuspect(); }}>
            <TextInput
              label="Suspect"
              placeholder="Player name, then Enter"
              leftSection={<IconSearch size={16} />}
              value={suspectName}
              onChange={(event) => setSuspectName(event.currentTarget.value)}
              error={suspectError}
            />
          </form>
        </Group>
      </Card>

      {history.error && (
        <Alert color="red" icon={<IconAlertCircle />} title="Couldn't load history">{history.error.message}</Alert>
      )}

      <Card withBorder padding={0}>
        {history.isLoading ? (
          <Center py="xl"><Loader /></Center>
        ) : reports.length === 0 ? (
          <Center py="xl"><Text c="dimmed" size="sm">No reports match these filters.</Text></Center>
        ) : (
          <Table.ScrollContainer minWidth={760}>
            <Table highlightOnHover verticalSpacing="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Suspect</Table.Th>
                  <Table.Th>Type</Table.Th>
                  <Table.Th>Reasons</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th>Handler</Table.Th>
                  <Table.Th>Opened</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {reports.map((report) => (
                  <Table.Tr key={report.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/reports/${report.id}`)}>
                    <Table.Td><PlayerName uuid={report.suspectUuid} /></Table.Td>
                    <Table.Td><GroupBadge group={report.group} /></Table.Td>
                    <Table.Td><CategoryBadges reasons={report.reasons} /></Table.Td>
                    <Table.Td><StatusBadge status={report.status} /></Table.Td>
                    <Table.Td>{report.handler ? <PlayerName uuid={report.handler} /> : <Text c="dimmed" size="sm">—</Text>}</Table.Td>
                    <Table.Td><Text size="sm" c="dimmed">{dateTime(report.createdAt)}</Text></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Card>

      {history.hasNextPage && (
        <Center>
          <Button variant="default" loading={history.isFetchingNextPage} onClick={() => void history.fetchNextPage()}>
            Load older reports
          </Button>
        </Center>
      )}
    </Stack>
  );
}
