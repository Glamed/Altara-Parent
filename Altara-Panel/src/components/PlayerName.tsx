import { Avatar, Group, Text, type MantineSize } from '@mantine/core';
import { usePlayer } from '../api/reports';
import { shortId } from '../format';

interface PlayerNameProps {
  uuid: string | null | undefined;
  size?: MantineSize;
  /** Shown when uuid is null (e.g. an automated reporter). */
  fallback?: string;
  withAvatar?: boolean;
}

export function PlayerName({ uuid, size = 'sm', fallback = 'Unknown', withAvatar = true }: PlayerNameProps) {
  const { data } = usePlayer(uuid);
  const label = uuid ? data?.name ?? shortId(uuid) : fallback;

  if (!withAvatar || !uuid) {
    return <Text size={size} fw={500} span>{label}</Text>;
  }

  return (
    <Group gap={8} wrap="nowrap">
      <Avatar src={`https://mc-heads.net/avatar/${uuid}/64`} size={size === 'lg' ? 40 : 24} radius="sm" />
      <Text size={size} fw={500} truncate>{label}</Text>
    </Group>
  );
}

/** Plain-text name, for places that need a string (notifications, spotlight). */
export function usePlayerLabel(uuid: string | null | undefined) {
  const { data } = usePlayer(uuid);
  return uuid ? data?.name ?? shortId(uuid) : 'Unknown';
}
