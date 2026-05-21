# Altara (Learning Example)

A modular Minecraft network example built to demonstrate how multiple server components can communicate through shared services like Redis.

This project is designed as a **learning reference** for structuring a multi-module Minecraft network codebase.

---

## What This Project Demonstrates

- Splitting a Minecraft network into multiple modules
- Sharing common code between platforms (Paper, Proxy, etc.)
- Sending network updates/events through Redis
- Organizing commands, listeners, and task systems in a clean structure
- Keeping module responsibilities separated and maintainable

---

## Module Overview

- `Altara-Shared`  
  Shared core logic, common models, utilities, and service abstractions.

- `Altara-Paper`  
  Paper-side runtime logic (commands, tasks, server monitoring, packet publishing).

- `Altara-Proxy`  
  Proxy-side integration for cross-server behavior and network-level listeners.

- `Altara-Lobby`  
  Lobby-specific plugin behavior and command handling.

- `Altara-Games`  
  Game-focused systems (states, teams, players, game flow, event architecture).

- `Altara-Web`  
  Placeholder/foundation module for web-related integration.

---

## Why Modular?

A modular setup makes it easier to:

- Reuse shared functionality
- Keep platform-specific code isolated
- Scale the project as the network grows
- Test and reason about each part independently

---

## Redis in This Example

Redis is used as a lightweight communication layer between components.  
Typical usage in this style of project includes:

- Broadcasting server state updates
- Synchronizing network events
- Sharing lightweight, real-time data between instances

Think of Redis here as the message bridge between different parts of the network.

---

## Learning Goals

If you're studying this project, focus on:

1. How shared code is separated from runtime-specific code
2. How modules communicate through packets/messages
3. How periodic tasks keep server/network state fresh
4. How command systems are registered per runtime
5. How to keep architecture clean as features grow

---

## Typical Development Flow

1. Add shared models/contracts in `Altara-Shared`
2. Implement runtime behavior in `Altara-Paper` or `Altara-Proxy`
3. Publish or consume network updates through Redis
4. Keep game/lobby features in their own modules
5. Iterate module-by-module without tightly coupling everything

---

## Build

From the root project:

```bash
mvn clean install