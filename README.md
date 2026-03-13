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

| Feature | Free | Mid (₦300/mo) | Pro (₦700/mo) |
|---|---|---|---|
| Offline text | ✅ | ✅ | ✅ |
| Photos / Audio | ❌ | ✅ | ✅ |
| File transfers | ❌ | ≤ 10MB | Unlimited |
| Eco Mode | Auto 42°C | Auto 42°C | Custom threshold |
| Desktop mirroring | ❌ | ❌ | ✅ |
| Offline payments | ❌ | ❌ | ✅ |

> Payment integration (Phase 7) uses NIBSS offline protocols + Moniepoint Paycode API.

---

## Roadmap

- [x] **Phase 1** — Foundation: KMP scaffold, GitHub repos, GitHub Pages, tier stubs
- [ ] **Phase 2** — Discovery Engine: Bluetooth LE + Wi-Fi Direct handshake
- [ ] **Phase 3** — Eco Mode Safety: 42°C thermal protocol
- [ ] **Phase 4** — Mesh Messaging & File Sharing
- [ ] **Phase 5** — Cross-Device Session (Phone ↔ Laptop sync)
- [ ] **Phase 6** — UI/UX Polish
- [ ] **Phase 7** — Monetisation (NIBSS + Moniepoint)

---

*The mesh engine is proprietary — available for licensing. Contact: [brytebee on GitHub](https://github.com/brytebee)*
