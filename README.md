# EcoMesh 🌐

**Offline-first, peer-to-peer mesh communication with built-in hardware safety.**

> Built for Nigeria — keeps phones cool, keeps people connected.

[![GitHub Pages](https://img.shields.io/badge/Live%20Demo-GitHub%20Pages-blue)](https://brytebee.github.io/EcoMesh-UI)

---

## What is EcoMesh?

EcoMesh lets smartphones and laptops communicate **without any internet connection, router, or cell tower** using Bluetooth LE and Wi-Fi Direct. Messages hop from device to device (mesh routing) until they reach the recipient.

### The Eco Mode Difference

Most mesh apps **drain batteries and overheat phones** — especially in Nigeria's 35°C+ climate. EcoMesh monitors the device's internal temperature in real time:

| Temperature | Action |
|---|---|
| < 38°C | Full performance — normal scanning |
| 38–40°C | Reduce scan frequency |
| 40–42°C | **Eco Mode** — dim screen, slow transfers, switch to BLE |
| > 42°C | Stop relaying — text-only, protect hardware |

---

## Architecture

```
EcoMesh-UI (this repo — public)     EcoMesh-Core (private)
├── common-ui/                       ├── mesh-core/
│   ├── Android                      │   ├── Bluetooth LE engine
│   ├── iOS                          │   ├── Wi-Fi Direct transport
│   ├── Desktop (JVM)                │   ├── Eco Mode thermal protocol
│   └── Web (Wasm → GitHub Pages)    │   └── SQLDelight message store
├── desktop-app/                     └── AccountManager (tier stubs)
└── mobile-app/
```

**Stack:** Kotlin Multiplatform · Compose Multiplatform · SQLDelight · Bluetooth LE · Wi-Fi Direct

---

## Account Tiers

| Feature | Free | Standard (₦300/mo) | Pro (₦700/mo) |
|---|---|---|---|
| Offline text | ✅ | ✅ | ✅ |
| Photos / Audio | ❌ | ✅ | ✅ |
| File transfers | ❌ | ≤ 10MB | Unlimited |
| Eco Mode | Auto 42°C | Auto 42°C | Custom threshold |
| Desktop mirroring | ❌ | ❌ | ✅ |
| Offline payments | ❌ | ❌ | ✅ |

> Payment integration (Phase 7) uses NIBSS offline protocols + Moniepoint Paycode API.

---

## Real-World Use Cases

- **Cross-Device Workspace Synchronization:** Seamlessly sync files, code, or build artifacts between different environments (e.g., a development MacBook and a Windows build machine) without needing to route through external internet servers. This solves the exact friction of moving local projects across devices for building and testing.
- **Off-Grid Connectivity:** Chat or share files in areas with zero internet or cellular coverage using peer-to-peer mesh routing.

---

## Getting Started & Running

The project contains launcher scripts to automatically build and deploy the Android app to a connected device via `adb` and run the JVM Desktop app concurrently.

### 💻 macOS (MacBook)
1. Ensure your Android device is connected via USB with USB Debugging enabled.
2. Initialize environment variable paths (e.g., in `~/.zshrc`) as described in the **[MacBook Setup & Migration Guide](file:///c:/Users/RevFavour/Documents/dev/eco-mesh/EcoMesh-Core/docs/macbook_setup_guide.md)**.
3. Make the script executable and run:
   ```bash
   chmod +x run_mesh.sh
   ./run_mesh.sh
   ```

### 🪟 Windows
1. Open PowerShell and run:
   ```powershell
   .\run_mesh.ps1
   ```

---

## Roadmap

- [x] **Phase 1** — Foundation: KMP scaffold, GitHub repos, GitHub Pages, tier stubs
- [x] **Phase 2** — Discovery Engine: Dual BLE and mDNS (Network Service Discovery) bridge
- [x] **Phase 3** — Eco Mode Safety: 42°C thermal polling protocol, scan throttling, cooling banners
- [x] **Phase 4** — Secure Handshake & Messaging: Curve25519 E2EE keys, TCP message framing, multi-peer sessions, and chat UI
- [x] **Phase 5** — Resumable File Transfers & DSR: Memory-safe chunk streaming, atomic TCP mutex locks, 2-way resumes, and non-flooding Dynamic Source Routing (DSR)
- [x] **Phase 6** — UI/UX Redesign: Silicon Valley obsidian glassmorphism theme, pulsing radar canvas, and compact action pills
- [ ] **Phase 7** — Monetisation: NIBSS offline protocol + Moniepoint Paycode API integration

---

*The mesh engine is proprietary — available for licensing. Detailed core documentation can be found in the `EcoMesh-Core/docs/` directory. Contact: [brytebee on GitHub](https://github.com/brytebee)*

