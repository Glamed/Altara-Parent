# Altara Staff Panel (example)

A React + Mantine example panel for the Altara report API: a live queue for a second
screen, report detail with chat logs, history search, and a Ctrl/⌘+K command palette.

## Run it

```bash
cp .env.example .env      # set ALTARA_BACKEND_KEY to config.json's backendKey
npm install
npm run dev               # http://localhost:5173
```

The Vite dev server proxies `/api` to Altara-Web and attaches the backend key itself, so
the key never reaches the browser. **This is a dev-only setup.** Before real staff use it,
put a small backend in front of the API that does the same proxying *and* real staff login
(e.g. Discord OAuth → staff profile). The sign-in screen here just trusts a typed name.

## What the buttons do

| Button | API | In-game effect |
| --- | --- | --- |
| Handle next report | `POST /api/report/engage-next` | Staff mode, report panel, sent to the suspect |
| Handle in-game / Go in-game | `POST /api/report/{id}/engage` | Same, for that report |
| Claim on web | `POST /api/report/{id}/claim` | None — handle it entirely from the panel |
| Accept / Reject | `POST /api/report/{id}/resolve` | In-game handler is told it was closed |
| Release | `POST /api/report/{id}/release` | Back in the queue |

If you're already holding another report, engaging asks before switching (`force: true`).
While you hold a report the panel sends `POST /api/report/{id}/heartbeat` every minute so a
web-only claim isn't released as abandoned.

## Layout

- `src/theme.ts` — `brand` (Altara aqua) and `discord` palettes; `defaultColorScheme="dark"` is set in `main.tsx`.
- `src/api/` — fetch client, types, TanStack Query hooks for every endpoint.
- `src/components/AppSpotlight.tsx` — the command palette (`@mantine/spotlight`).
- `src/pages/` — Queue, Report, History, Sign-in.

Avatars come from mc-heads.net.
