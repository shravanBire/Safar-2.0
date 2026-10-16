# AGENTS.md — AI Collaborator & Developer Master Guide for SAFAR 2.0

> **INSTRUCTION FOR AI AGENTS:** Jab bhi tum is project me code likhe ya modify kare, pehle is file ko padhe. Hum SAFAR 1.0 (Supabase cloud prototype) ko **SAFAR 2.0 (Serverless P2P + Local RoomDB + Royal Enfield Retro-Moto UI)** me upgrade kar rahe hain.

## 1. SAFAR 1.0 Bugs That Must NEVER Be Repeated
1. **NO Hardcoded Cloud Keys:** `SupabaseClientProvider.kt` aur Supabase dependencies (`postgrest-kt`, `realtime-kt`) ko completely remove karna hai.
2. **NO Multiple ViewModel Instantiations:** SAFAR 1.0 me `HomeScreen`, `TripsScreen`, aur `SettingScreen` apna alag `DeviceRepository` banate the. SAFAR 2.0 me shared ViewModels use honge.
3. **FIX Unit Conversion:** `UnitViewModel` me `mph` select hone par sirf text label nahi badalna hai; har speed aur distance value ko actual mathematical conversion (`* 0.621371`) se convert karna hai.
4. **FIX MapLibre Polyline Updates:** `MapView.kt` ke `OpenStreetMapViewWithPolyline` me polyline `factory` block ke andar nahi, balki `update` block ke andar draw karni hai.
5. **PERSIST Settings:** Theme aur Unit ko `DataStore` me save karna hai taaki app restart par reset na ho.

---

## 2. Target Android Package Structure (`com.example.safar`)
```text
com.example.safar/
├── data/
│   ├── local/
│   │   ├── SafarDatabase.kt          # RoomDatabase (@Database version = 1)
│   │   ├── dao/                      # DeviceDao & TripDao
│   │   ├── entity/                   # DeviceEntity, TripSummaryEntity, LocationPointEntity
│   │   └── PreferencesDataStore.kt   # Persists Theme & Unit
│   ├── network/
│   │   ├── NostrSignalingClient.kt   # OkHttp WebSocket + SHA256 Relay Sharding
│   │   ├── UdpHolePunchEngine.kt     # IPv6 DatagramChannel P2P Engine
│   │   └── BinaryPacketCodec.kt      # Parses 16B TripPoint & 488B History Chunks + CRC16
│   └── repository/
│       ├── DeviceRepository.kt       # RoomDB CRUD + QR URI parser
│       └── TelemetryRepository.kt    # Coordinates P2P Live Stream & History Sync
├── ui/
│   ├── theme/                        # Retro-Moto Palette (Obsidian, Brass, Copper)
│   ├── components/                   # TripperSpeedometer, TelemetryHeaderPill, RideTicketCard
│   └── screens/                      # home, trips, settings
├── viewModels/                       # DeviceViewModel, CockpitViewModel, TripsViewModel
└── MainActivity.kt