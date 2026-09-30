import { useState } from 'react';
import { Button, Center, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconLogin2, IconShieldHalfFilled } from '@tabler/icons-react';
import { lookupPlayerByName } from '../api/reports';
import { ApiError } from '../api/client';
import { useStaff } from '../staff/StaffContext';

export function SignInPage() {
  const { signIn } = useStaff();
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!name.trim()) return;
    setLoading(true);
    setError(null);
    try {
      signIn(await lookupPlayerByName(name.trim()));
    } catch (e) {
      setError(e instanceof ApiError && e.status === 404 ? 'No player with that name has joined Altara.' : 'Could not reach the API.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Center h="100vh" p="md">
      <Paper withBorder p="xl" w={380} maw="100%">
        <form onSubmit={(event) => { event.preventDefault(); void submit(); }}>
          <Stack>
            <Stack gap={4} align="center">
              <IconShieldHalfFilled size={40} color="var(--mantine-color-brand-5)" />
              <Title order={3}>Altara Staff Panel</Title>
              <Text size="sm" c="dimmed" ta="center">
                Example sign-in: enter your Minecraft name. Replace this with real auth before shipping.
              </Text>
            </Stack>
            <TextInput
              label="Minecraft name"
              placeholder="Notch"
              value={name}
              onChange={(event) => setName(event.currentTarget.value)}
              error={error}
              autoFocus
            />
            <Button type="submit" leftSection={<IconLogin2 size={18} />} loading={loading} fullWidth>
              Continue
            </Button>
          </Stack>
        </form>
      </Paper>
    </Center>
  );
}
