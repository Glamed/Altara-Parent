import { AppShell, Burger, Group, Kbd, Menu, NavLink, Text, UnstyledButton } from '@mantine/core';
import { useDisclosure, useInterval } from '@mantine/hooks';
import { spotlight } from '@mantine/spotlight';
import { IconChevronDown, IconHistory, IconListDetails, IconLogout, IconSearch, IconShieldHalfFilled } from '@tabler/icons-react';
import { useEffect } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { heartbeat, useHandling } from '../api/reports';
import { useSignedInStaff, useStaff } from '../staff/StaffContext';
import { AppSpotlight } from './AppSpotlight';
import { PlayerName } from './PlayerName';

const HEARTBEAT_MS = 60_000;

/**
 * While the panel is open and you hold a report, ping the API so it isn't treated as
 * abandoned (the API releases web-only claims after ~5 minutes of silence).
 */
function useReportHeartbeat() {
  const staff = useSignedInStaff();
  const { data: handling } = useHandling(staff.uuid);
  const reportId = handling?.id;

  const interval = useInterval(() => {
    if (reportId) void heartbeat(reportId, staff.uuid).catch(() => undefined);
  }, HEARTBEAT_MS);

  useEffect(() => {
    if (!reportId) return;
    interval.start();
    return interval.stop;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportId]);
}

export function Layout() {
  const [opened, { toggle, close }] = useDisclosure();
  const { pathname } = useLocation();
  const { signOut } = useStaff();
  const staff = useSignedInStaff();
  useReportHeartbeat();

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 220, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group gap="xs">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <IconShieldHalfFilled size={26} color="var(--mantine-color-brand-5)" />
            <Text fw={700}>Altara Staff</Text>
          </Group>

          <Group gap="sm">
            <UnstyledButton onClick={spotlight.open} visibleFrom="sm">
              <Group gap={6} px="sm" py={6} style={{ border: '1px solid var(--mantine-color-default-border)', borderRadius: 'var(--mantine-radius-md)' }}>
                <IconSearch size={16} />
                <Text size="sm" c="dimmed" mr="lg">Search</Text>
                <Kbd size="xs">Ctrl K</Kbd>
              </Group>
            </UnstyledButton>

            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <UnstyledButton>
                  <Group gap={4}>
                    <PlayerName uuid={staff.uuid} />
                    <IconChevronDown size={14} />
                  </Group>
                </UnstyledButton>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item leftSection={<IconLogout size={16} />} onClick={signOut}>Sign out</Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm">
        <NavLink
          component={Link}
          to="/"
          label="Queue"
          leftSection={<IconListDetails size={18} />}
          active={pathname === '/' || pathname.startsWith('/reports')}
          onClick={close}
        />
        <NavLink
          component={Link}
          to="/history"
          label="History"
          leftSection={<IconHistory size={18} />}
          active={pathname.startsWith('/history')}
          onClick={close}
        />
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>

      <AppSpotlight />
    </AppShell>
  );
}
