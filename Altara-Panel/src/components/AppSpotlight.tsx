import { Spotlight, type SpotlightActionData, type SpotlightActionGroupData } from '@mantine/spotlight';
import {
  IconHistory, IconListDetails, IconPlayerTrackNext, IconSearch, IconUserSearch,
} from '@tabler/icons-react';
import { useQueries } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { get } from '../api/client';
import { keys, useQueue } from '../api/reports';
import type { PlayerRef } from '../api/types';
import { shortId, titleCase } from '../format';
import { useEngageFlow } from './useEngageFlow';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Command palette (Ctrl/⌘ + K): navigation, "handle next", every open report, and player/report lookup. */
export function AppSpotlight() {
  const navigate = useNavigate();
  const { engage } = useEngageFlow();
  const { data: queue } = useQueue();
  const [query, setQuery] = useState('');

  const open = [...(queue?.pending ?? []), ...(queue?.inProgress ?? [])];

  // Names for every suspect in the queue, so they're searchable (shares the PlayerName cache).
  const names = useQueries({
    queries: open.map((report) => ({
      queryKey: keys.player(report.suspectUuid),
      queryFn: () => get<PlayerRef>(`/api/uuid/${report.suspectUuid}`),
      staleTime: Infinity,
      retry: false,
    })),
  });

  const general: SpotlightActionGroupData = {
    group: 'Go to',
    actions: [
      {
        id: 'queue',
        label: 'Queue',
        description: 'Pending and in-progress reports',
        leftSection: <IconListDetails size={20} />,
        onClick: () => navigate('/'),
      },
      {
        id: 'history',
        label: 'History',
        description: 'Search every report',
        leftSection: <IconHistory size={20} />,
        onClick: () => navigate('/history'),
      },
      {
        id: 'engage-next',
        label: 'Handle next report',
        description: 'Claim the top of the queue and get sent to the suspect in-game',
        leftSection: <IconPlayerTrackNext size={20} />,
        keywords: ['claim', 'next', 'engage'],
        onClick: () => engage(),
      },
    ],
  };

  const reports: SpotlightActionGroupData = {
    group: 'Open reports',
    actions: open.map((report, index): SpotlightActionData => {
      const name = names[index]?.data?.name ?? shortId(report.suspectUuid);
      const categories = [...new Set(report.reasons.map((reason) => titleCase(reason.category)))].join(', ');
      return {
        id: `report-${report.id}`,
        label: name,
        description: `${titleCase(report.status)} · ${categories}`,
        keywords: [report.id, report.suspectUuid],
        leftSection: <IconListDetails size={20} />,
        onClick: () => navigate(`/reports/${report.id}`),
      };
    }),
  };

  // Always-available lookups for whatever was typed.
  const typed = query.trim();
  const lookups: SpotlightActionGroupData = {
    group: 'Look up',
    actions: typed
      ? [
          {
            id: 'lookup-player',
            label: `Reports against "${typed}"`,
            description: 'Search history by player name',
            leftSection: <IconUserSearch size={20} />,
            keywords: [typed],
            onClick: () => navigate(`/history?suspect=${encodeURIComponent(typed)}`),
          },
          ...(UUID_PATTERN.test(typed)
            ? [{
                id: 'lookup-report',
                label: `Open report ${shortId(typed)}`,
                leftSection: <IconSearch size={20} />,
                keywords: [typed],
                onClick: () => navigate(`/reports/${typed}`),
              }]
            : []),
        ]
      : [],
  };

  return (
    <Spotlight
      actions={[general, reports, lookups].filter((group) => group.actions.length > 0)}
      query={query}
      onQueryChange={setQuery}
      nothingFound="Nothing found"
      highlightQuery
      limit={12}
      shortcut={['mod + K', '/']}
      searchProps={{ leftSection: <IconSearch size={20} />, placeholder: 'Search reports, players, actions…' }}
    />
  );
}
