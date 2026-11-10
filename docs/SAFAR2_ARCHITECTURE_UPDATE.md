# SAFAR 2.0 — Architecture Update & Agent Handoff
**Revision date:** 2026-11-09
**Priority:** If this file conflicts with `PRD.md`, `API_SPEC_AND_DB_SCHEMA.md` (incl. ADRs), `AGENTS.md` or the System Architecture doc, **THIS FILE WINS**. Do not "fix" the code back to the old docs.

---

## 1. Why the architecture changed
- The college could not arrange the **public-static-IP M2M SIM**.
- **UDP hole punching was tested and is not reliable** (mobile carriers use CGNAT/firewalls; inbound packets from the phone cannot reach the tracker).
- The teacher's instruction: *build a working project before evaluation; everything else later.*
- Hardware status: all hardware ready **except the SIM/LTE module (A7670C)**. Work proceeds over **Wi-Fi first**; the SIM is added later with no app-side redesign.

## 2. New architecture: "Edge storage + relay-only (zero-data) server"

```
ESP32 tracker  ──outbound (MQTT over TLS)──▶  RELAY (MQTT broker)  ◀──outbound (MQTT over TLS)──  Android app
 • SRAM→LittleFS trip log                      • forwards messages only                          • RoomDB (local history)
 • latest-position entry                       • stores NO trip data                             • live UI, sync, ACK
 • autonomous GSM SOS                          • cannot read payloads (E2E encrypted)
```

Key facts:
1. **Both sides only make OUTBOUND connections.** This works behind any NAT/CGNAT and with any SIM. The relay needs a public address; the tracker does not.
2. The relay is a plain **MQTT broker**. Prototype: a free hosted tier (e.g. HiveMQ Cloud / EMQX Cloud free; verify current limits) or self-hosted **Mosquitto** on a free VM / Raspberry Pi. **Broker URL/credentials must be configurable** (never hardcoded in source).
3. **Trip history still lives on the ESP32** (edge storage). The server never stores history.
4. **Payloads stay end-to-end encrypted** with the 256-bit `shared_secret` from the pairing QR. The broker only sees ciphertext.
5. **Transport is swappable: Wi-Fi now, SIM later.** MQTT code is identical over Wi-Fi and over the A7670C (PPPoS). Only the network bring-up layer changes. IPv4 TCP is enough (no IPv6 requirement).
6. **Production path:** one own relay (VPS or Raspberry Pi with public address); every tracker uses any normal SIM. A static-IP M2M SIM is NOT required.

## 3. What is deprecated / removed (do NOT implement)
- IPv6 UDP hole punching, `UdpHolePunchEngine`, CoAP, heartbeat-for-NAT-pinhole (25 s UDP keepalive).
- `NostrSignalingClient`, public Nostr relays, Kind 20000 fallback, relay sharding `SHA256(pub) % 50` (ADR-04 is void).
- `secp256k1` Nostr event signing (message authenticity now comes from AEAD).
- `App_IPv6` and `Port` fields in `PKT_OPEN_SESSION`.
- Wording "Serverless P2P" / "Zero-Cloud" in docs and UI copy. Use: **"Edge storage + relay-only server"**.
- ADR-01 is amended: data lives on ESP32, but a forwarding relay now exists.

## 4. What stays unchanged
- All Retro-Moto UI (theme, `TripperSpeedometer`, `RideTicketCard`, `IgnitionSplash`, Logbook, Settings).
- RoomDB schema (`devices`, `trip_summaries`, `location_points`) and DataStore (theme, unit).
- 16-byte `TripPoint` struct, opcode concept (0x01–0x05), history chunk idea (30 points/chunk).
- Edge design: 4 KB SRAM ring buffer → LittleFS batch flush (ADR-03).
- **Autonomous GSM crash SOS** (ADR-02): ESP32 calls (`ATD`) and sends SMS (`AT+CMGS`) by itself; no server, no app needed.
- QR pairing flow (`safar://pair?...`), unit conversion (`* 0.621371`), all "never repeat" rules from `AGENTS.md` section 1.

## 5. MQTT design (PROPOSED — freeze after the Sprint 4 spike; do not diverge silently)

**Topic prefix:** `safar/v1/<dev>/` where `<dev>` = first 16 hex chars of `SHA256(pub)`; `pub` from the QR is now just an opaque device identity.

| Topic | Direction | QoS | Purpose |
|---|---|---|---|
| `.../status` | ESP32 → app | 1, **retained**, with **Last Will** | online/offline + last known position (instant position when app opens) |
| `.../live` | ESP32 → app | 0 | 1 Hz live points, only while app is "watching" |
| `.../cmd` | app → ESP32 | 1 | OPEN_SESSION (with `Last_Synced_TS`), watch-heartbeat, HISTORY_ACK |
| `.../hist` | ESP32 → app | 1 | history chunks |

Behaviour:
- **Retained `status`:** ESP32 publishes latest position on ignition-off, on parked/riding state change, and at a low configurable rate while riding (default 30–60 s). This is the "single latest entry" the app reads on open.
- **Live stream** starts on OPEN_SESSION and stops if no app watch-heartbeat arrives for ~30 s (saves SIM data; roughly 0.3–0.4 MB per hour of live viewing).
- **History sync:** app sends `Last_Synced_TS` → ESP32 sends chunks → app saves to RoomDB **in one transaction** → app sends HISTORY_ACK.
- **Delete rule (strict):** ESP32 deletes synced trip data **only after** the ACK for that chunk, and the app sends ACK **only after** the Room commit succeeded. Known limitation: single paired phone assumed (two phones would split the data). Optionally keep a retention window instead of immediate delete.

**Packet framing (PROPOSED):**
- All sizes **include the 1-byte opcode** (resolves the old 20 vs 21 / 488 vs 512 confusion). Plain frames: live = 21 B, history chunk = 488 B.
- Encrypted frame = `opcode(1) | nonce(12) | ciphertext | tag(16)` using ChaCha20-Poly1305 (inner = old payload without CRC). Live = 47 B, history = 514 B. **CRC16 is dropped when AEAD is on** (kept only for a plain debug mode).
- **Nonce must never repeat under the same key.** Use a random 12-byte nonce (ESP32 hardware RNG); do NOT use a counter that resets on reboot.
- Replay protection: inner payload carries timestamp + 2-byte sequence; receiver keeps a window and rejects old/duplicate frames. Sequence (2 B) wraps in ~18 h at 1 Hz, so never rely on it alone.
- CRC16 variant (debug mode) = CCITT-FALSE: poly 0x1021, init 0xFFFF, no reflection, stored little-endian.
- Create **golden test vectors** (byte arrays + expected parsed values) shared by Android unit tests and ESP32 firmware tests.

## 6. Wi-Fi-first plan and SOS
- ESP32 connects to Wi-Fi (or phone hotspot) and to the broker now; later the A7670C (PPPoS) replaces Wi-Fi under the same MQTT layer.
- **SOS until SIM arrives:** implement full crash detection state machine (impact > 3.5 g + tilt > 60° for 10 s → 15 s buzzer/cancel window) and replace the GSM actions with a **stub** (serial log + buzzer pattern + "SOS would fire" message). Switch the stub for real `ATD` + `AT+CMGS` when the SIM module is in hand. Do not claim SOS delivery works until tested with a real SIM.

## 7. Current status (verified in code, 2026-11-09)
**Done:** RoomDB + DAOs, DataStore, `BinaryPacketCodec`, `MockEsp32Simulator`, Retro-Moto UI overhaul, QR scanner (CameraX + ML Kit) with one-shot scan guard (`hasScanned`), QR-text and manual pairing fallback.
**Firmware:** reported as not started (verify). **No MQTT code exists yet** in the app.

**Known open issues (from code review), by priority:**
1. **Map gestures:** panning the map scrolls the whole Home screen (needs `requestDisallowInterceptTouchEvent`); camera re-centers on the bike on every live update (needs follow/recenter mode); expand toggle uses `.clickable` on the map container; MapView is not paused/stopped/destroyed on dispose.
2. **Pairing bugs:** `sim=+91...` loses `+` (decoded as space); `pub`/`sec` not validated (must be 64 hex chars); `onError` is empty in `HomeScreen`; `lastSyncedTimestamp` is set to *now* at pairing (must be **0** for a new device or history will never sync).
3. **Codec:** truncated packets are marked CRC-valid (`else true`); history `count > 30` not rejected; opcode is optional/guessed; encode uses truncation instead of rounding; no range checks; **no unit tests**.
4. **Security:** `allowBackup="true"` with plaintext `shared_secret_hex` in Room; demo keys inside `QrPairingDialog`; shared secret should move to Android Keystore-backed storage.
5. **Dev leftovers:** `seedMockData` runs on every launch; `liveOdoDistanceKm` and Home TRIP/AVG/MAX are hardcoded fake values; GPS last-known write hits Room every second (throttle); connection freshness uses device timestamp (use local receive time).
6. **Build:** Room `2.7.0-alpha11` (move to stable); `navigation-compose` unused.

## 8. Next tasks (in order)
1. Update docs (section 10) and freeze wire framing with golden vectors; write `BinaryPacketCodecTest` (roundtrip, truncated, bad CRC, count > 30, negative lat/lon).
2. Fix issues 1 and 2 above (map, pairing).
3. Introduce a `TelemetrySource` interface; `MockTelemetrySource` (current simulator) + `MqttTelemetrySource`. UI/ViewModel must not know which one is active. Remove the direct dependency of `SafarRepository` on `MockEsp32Simulator`.
4. Pick broker; test phone ↔ broker with a desktop MQTT client. Config for broker host/credentials (not hardcoded).
5. App: MQTT client, live `status`/`live` topics → UI, encryption layer, connection states.
6. ESP32 (Wi-Fi first): publish real GPS points, retained status, LittleFS logging, history chunks + ACK + delete-after-ACK.
7. Crash SOS state machine with stub; swap to real GSM when SIM arrives.
8. Move A7670C (PPPoS) under the same MQTT layer.

## 9. Rules for every agent working on this project
- Package is **`com.example.safar20`** (not `com.example.safar`).
- Never reintroduce Supabase, Nostr, UDP hole punching, or hardcoded keys/credentials.
- One shared `SafarViewModel`/`SafarRepository`; no per-screen duplicates (split by feature only when asked).
- Units: real mathematical conversion everywhere; persist theme/units in DataStore.
- Do not change UI files when the task is networking, and vice versa. Touch only the files named in the task.
- No `curl`/shell commands inside prompts meant for the Windows Android Studio agent.
- Do not invent new wire formats: any packet change must be reflected here first.
- If something here is unclear or conflicts with code, ask the project owner instead of guessing.

## 10. Docs that must be updated to match this file
`PRD.md` (vision, business model wording, SRS FR-02), `API_SPEC_AND_DB_SCHEMA.md` (OPEN_SESSION fields, packet sizes, encryption framing, ADR-01, ADR-04), `AGENTS.md` (package structure: replace `UdpHolePunchEngine`/`NostrSignalingClient` with `MqttTelemetrySource`, add `TelemetrySource`), System Architecture doc (data flow section 3, security section 4).

## 11. Open questions for the project owner
- Which broker for the demo (hosted free tier vs own Mosquitto)? Keep a self-hosted backup ready for evaluation day.
- Live position rate while riding with the app closed (default 30–60 s) — acceptable SIM data use?
- Immediate delete after sync vs retention window on the ESP32?
- Evaluation date (decides how much of steps 6–8 is demo-critical).
