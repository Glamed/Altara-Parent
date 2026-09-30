import { Text } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';
import { IconCheck, IconX } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';
import { useEngage } from '../api/reports';
import type { EngageResult } from '../api/types';
import { useSignedInStaff } from '../staff/StaffContext';

const FAILURES: Record<string, { title: string; message: string }> = {
  STAFF_OFFLINE: { title: "You're not in-game", message: 'Join the network first — or use "Claim on web" to handle it from here.' },
  TAKEN: { title: 'Already taken', message: 'Someone else is handling that report.' },
  CLOSED: { title: 'Report closed', message: 'That report has already been resolved.' },
  NOT_FOUND: { title: 'Not found', message: 'That report no longer exists.' },
  EMPTY: { title: 'Queue is empty', message: 'Nothing waiting right now.' },
};

/**
 * "Handle in-game": claims the report and has the API tell your server to put you in staff
 * mode, show the report and send you to the suspect. Handles the BUSY case by asking whether
 * to drop the report you're already holding.
 */
export function useEngageFlow() {
  const staff = useSignedInStaff();
  const engage = useEngage();
  const navigate = useNavigate();

  const handle = (result: EngageResult, reportId: string | undefined) => {
    switch (result.outcome) {
      case 'ENGAGED':
        notifications.show({
          color: 'green',
          icon: <IconCheck size={18} />,
          title: 'Sent in-game',
          message: "You've been put in staff mode and sent to the suspect.",
        });
        if (result.report) navigate(`/reports/${result.report.id}`);
        return;

      case 'BUSY':
        modals.openConfirmModal({
          title: 'You already have a report',
          children: (
            <Text size="sm">
              You're still handling another report. Put it back in the queue and switch to this one?
            </Text>
          ),
          labels: { confirm: 'Switch', cancel: 'Keep current' },
          onConfirm: () => run(reportId, true),
        });
        return;

      default: {
        const failure = FAILURES[result.outcome];
        notifications.show({ color: 'red', icon: <IconX size={18} />, ...failure });
      }
    }
  };

  const run = (reportId?: string, force = false) =>
    engage.mutate(
      { staffUuid: staff.uuid, reportId, force },
      {
        onSuccess: (result) => handle(result, reportId),
        onError: (error) => notifications.show({ color: 'red', title: 'Request failed', message: error.message }),
      },
    );

  return {
    /** Engage a specific report, or the top of the queue when no id is given. */
    engage: (reportId?: string) => run(reportId),
    pending: engage.isPending,
  };
}
