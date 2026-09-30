import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import type { PlayerRef } from '../api/types';

/**
 * Who is using the panel. This example just remembers a Minecraft name you type in — a real
 * panel should get this from its own login (e.g. Discord OAuth mapped to a staff profile), and
 * that backend should be the one holding the API's backendKey.
 */
interface StaffContextValue {
  staff: PlayerRef | null;
  signIn: (staff: PlayerRef) => void;
  signOut: () => void;
}

const STORAGE_KEY = 'altara-panel-staff';
const StaffContext = createContext<StaffContextValue | null>(null);

function load(): PlayerRef | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as PlayerRef) : null;
  } catch {
    return null;
  }
}

function store(staff: PlayerRef | null) {
  try {
    if (staff) localStorage.setItem(STORAGE_KEY, JSON.stringify(staff));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage unavailable (private window etc.) — the session still works, it just won't persist.
  }
}

export function StaffProvider({ children }: { children: ReactNode }) {
  const [staff, setStaff] = useState<PlayerRef | null>(load);

  const signIn = useCallback((next: PlayerRef) => {
    store(next);
    setStaff(next);
  }, []);

  const signOut = useCallback(() => {
    store(null);
    setStaff(null);
  }, []);

  const value = useMemo(() => ({ staff, signIn, signOut }), [staff, signIn, signOut]);
  return <StaffContext.Provider value={value}>{children}</StaffContext.Provider>;
}

export function useStaff() {
  const context = useContext(StaffContext);
  if (!context) throw new Error('useStaff must be used inside <StaffProvider>');
  return context;
}

/** For components that only render once signed in. */
export function useSignedInStaff(): PlayerRef {
  const { staff } = useStaff();
  if (!staff) throw new Error('No staff member signed in');
  return staff;
}
