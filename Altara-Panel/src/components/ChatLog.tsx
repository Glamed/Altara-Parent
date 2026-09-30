import { Badge, Group, Paper, ScrollArea, Stack, Text, Tooltip } from '@mantine/core';
import type { ReportMessage } from '../api/types';
import { dateTime, timeAgo } from '../format';
import { PlayerName } from './PlayerName';

/** The chat captured with a chat report, oldest first; the suspect's lines are highlighted. */
export function ChatLog({ messages, suspectUuid }: { messages: ReportMessage[]; suspectUuid: string }) {
  if (messages.length === 0) {
    return <Text c="dimmed" size="sm">No chat was attached to this report.</Text>;
  }

  const ordered = [...messages].sort((a, b) => a.sentAt - b.sentAt);

  return (
    <ScrollArea.Autosize mah={420} type="auto">
      <Stack gap="xs">
        {ordered.map((message) => {
          const fromSuspect = message.senderUuid === suspectUuid;
          return (
            <Paper
              key={`${message.senderUuid}-${message.sentAt}`}
              p="sm"
              withBorder
              bg={fromSuspect ? 'var(--mantine-color-red-light)' : undefined}
            >
              <Group justify="space-between" wrap="nowrap" mb={4}>
                <PlayerName uuid={message.senderUuid} />
                <Group gap="xs" wrap="nowrap">
                  {message.reportedBy.length > 0 && (
                    <Badge color="red" variant="light" size="sm">
                      Reported by {message.reportedBy.length}
                    </Badge>
                  )}
                  <Tooltip label={dateTime(message.sentAt)}>
                    <Text size="xs" c="dimmed">{timeAgo(message.sentAt)}</Text>
                  </Tooltip>
                </Group>
              </Group>
              <Text size="sm" style={{ wordBreak: 'break-word' }}>{message.message}</Text>
            </Paper>
          );
        })}
      </Stack>
    </ScrollArea.Autosize>
  );
}
