# SAFAR 2.0 — Design System & UI/UX Style Guide
**Theme Inspiration:** Royal Enfield Tripper Dash / Vintage Cafe-Racer Instrument Cluster

## 1. Color Palette (Replacing SAFAR 1.0 Basic Green)
SAFAR 1.0 ke `Color.kt` aur `Theme.kt` me standard material green aur plain white/grey surfaces the. SAFAR 2.0 me hum **Retro Moto Cockpit** palette use karenge:

### Dark Theme (Default — "Midnight Moto Cockpit")
- **Primary Background (`Crankcase Black`):** `#0E0E10` (Deep matte metallic black)
- **Elevated Surface / Card (`Gunmetal Gauge`):** `#18181B` with `1.dp` border of `#2E2A24` (Brushed bronze rim)
- **Primary Accent (`Royal Brass / Amber Gold`):** `#D4AF37` (Speedometer needle, active tabs, primary buttons)
- **Secondary Accent (`Cafe Copper`):** `#C86D51` (Route polylines, secondary badges)
- **Telemetry Live Green (`Phosphor Green`):** `#4ADE80` (Active P2P UDP indicator)
- **Alert / SOS Red (`Signal Tail-Lamp Red`):** `#EF4444` (High-RPM redline zone & offline errors)
- **Text Primary (`Warm Ivory`):** `#F4EFE6` (Vintage dial numeral cream)
- **Text Secondary (`Muted Pewter`):** `#9CA3AF`

### Light Theme ("Classic Parchment Day-Dial")
- **Primary Background (`Vintage Parchment`):** `#F4EFE6` (Warm cream speedometer dial look)
- **Elevated Surface (`Warm Sand`):** `#EAE2D3` with `1.dp` border of `#C5B8A5`
- **Primary Accent (`British Racing Green`):** `#143B27` (Classic heritage green)
- **Secondary Accent (`Burnished Brass`):** `#B5892E`
- **Text Primary (`Espresso Ink`):** `#1C1917`

---

## 2. Typography (`Type.kt` Upgrade)
- **Display / Speedometer Numerals:** `JetBrains Mono` / `Bebas Neue` / `Oswald` (Bold, condensed mechanical odometer look)
- **Section Headers:** `Rajdhani` or `Space Grotesk` (SemiBold, uppercase with `1.5.sp` letter-spacing)
- **Body & Labels:** `Plus Jakarta Sans` or `Inter` (Clean legibility for maps and settings)

---

## 3. Core Component Library (`Screens/Components/`)

### A. `TripperSpeedometer.kt` (Upgrading `SpeedoMeter.kt`)
SAFAR 1.0 ke `SpeedoMeter.kt` me simple flat arcs the. SAFAR 2.0 me:
- **Outer Bezel:** Double metallic ring (`#2E2A24` outer, `#D4AF37` thin inner accent).
- **Dial Ticks:** 0 se 160 km/h tak crisp vintage dial ticks; 120+ km/h ke ticks `Signal Red (#EF4444)` me.
- **Animated Needle:** `animateFloatAsState` ke saath smooth mechanical spring damping needle.
- **Center Rolling Odometer:** Center me digital speed + niche mechanical-style `TRIP KM` counter.

### B. `TelemetryHeaderPill.kt` (New Component on `HomeScreen`)
- Top bar me `"Home"` (`44.sp` text in SAFAR 1.0) ki jagah ek sleek cockpit status bar:
    - Left: Bike Selector Dropdown pill (`"RE Classic 350 ▾"`).
    - Right: Live Link Badge (`"● P2P DIRECT (38ms)"` in Phosphor Green, `"● SYNCING LOGS (72%)"` in Amber Gold, or `"● PARKED (LAST SEEN 10m AGO)"`).

### C. `RideTicketCard.kt` (Upgrading `TripsScreen.kt`)
- SAFAR 1.0 me date chunne par sirf 4 plain text lines aati thi.
- SAFAR 2.0 me date select karte hi us din ki har ride ka ek **"Vintage Highway Ticket Card"** dikhega:
    - Header: `RIDE #02 • 06:15 PM → 06:50 PM (35 mins)`
    - 3-Column Brass Badge Grid: `18.4 KM` | `AVG 31 KM/H` | `MAX 78 KM/H`
    - Expandable Map Preview: Card par tap karte hi `MapLibre` par `Cafe Copper (#C86D51)` color ki smooth polyline render hogi.