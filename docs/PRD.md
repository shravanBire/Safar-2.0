# SAFAR 2.0 — Product Requirements Document (PRD) & SRS

## 1. Product Vision & Business Objective (BRD)
SAFAR 1.0 ek cloud-dependent prototype tha jo Supabase (`postgrest-kt` aur `realtime-kt`) par chalta tha. Uska sabse bada drawback tha recurring cloud database/bandwidth billing aur client-side filtering bottleneck.
**SAFAR 2.0** ek **Zero-Cloud, Serverless Peer-to-Peer (P2P) Smart Motorcycle Telemetry & Safety System** hai jo 100% free (sirf normal SIM recharge par) chalta hai aur 1 user se lekar 1,00,000+ users tak bina kisi server bill ke scale hota hai.

### Business Model: BYOS (Bring Your Own SIM)
- User hardware box me apna khud ka normal 4G SIM (Jio/Airtel/Vi) lagata hai.
- Hardware ke upar printed **QR Code** ko SAFAR Android App se scan karke pair karta hai.
- **Company Infrastructure Cost:** ₹0/month forever.

---

## 2. Competitor & Market Analysis
| Feature | Commercial Trackers (Ajjas, MapMyIndia) | SAFAR 1.0 (Legacy) | SAFAR 2.0 (Target) |
| :--- | :--- | :--- | :--- |
| **Yearly Subscription** | ₹1,500 – ₹3,000 / year | Supabase Free Tier limits | **₹0 Forever (No Cloud)** |
| **Offline Blind-Spot Logging** | Limited | None (Lost if offline) | **65,000+ Points in Internal Flash** |
| **SOS Alert Delivery** | App Push Notification (Needs Internet) | Not implemented | **Direct GSM Call + SMS (Offline Ready)** |
| **UI Aesthetic** | Generic corporate UI | Basic Material3 Green | **Royal Enfield Retro-Moto Cockpit** |

---

## 3. Target User Personas & User Stories
- **Persona 1: Daily Commuter / College Rider** — Chahta hai ki bike park karne ke baad chori ya girne par turant phone call aa jaye, aur roz ka mileage/speed record ho.
- **Persona 2: Weekend Highway Tourer (Royal Enfield / Cruiser Rider)** — Pahado aur zero-network zones me ride karta hai; chahta hai ki offline route bhi record ho aur baad me app kholne par pura map sync ho jaye.

### Core User Stories
- **US-01 (QR Pairing):** As a rider, main `AddDeviceDialog` me manual ID type karne ke badle QR code scan karke 2 second me apna tracker pair karna chahta hoon.
- **US-02 (Live Cockpit):** As a rider, jab main app kholun, mujhe 1–2 second me bike ki live location, retro brass speedometer par real-time speed, aur connection health dikhni chahiye.
- **US-03 (Ride-by-Ride History):** As a rider, main kisi bhi date par click karun toh mujhe us din ki alag-alag rides (Trip 1: Subah, Trip 2: Shaam) ke cards, distance, max speed, aur map polyline dikhne chahiye.
- **US-04 (App-Independent SOS):** As a rider, agar meri bike ka accident ho ya bike gire, toh bina app khule mere phone (aur emergency contact) par turant normal GSM Voice Call aur Google Maps link wala SMS aana chahiye.

---

## 4. Software Requirements Specification (SRS) & Acceptance Criteria
1. **FR-01 (Device Pairing):** App camera se QR scan karke `device_id`, `public_key`, `shared_secret`, aur `sim_number` local `RoomDB` me save karegi.
2. **FR-02 (On-Demand Live Stream):** App khulne par Nostr handshake ke through IPv6 UDP hole punch hoga aur 1 Hz (har second) par 16-byte GPS packet stream hoga.
3. **FR-03 (Flash Trip Sync):** Jab bike offline chal rahi ho, ESP32 `4KB SRAM` buffer ke through `LittleFS` me data save karega aur app connect hote hi 512-byte chunks me `RoomDB` me sync karega.
4. **FR-04 (Unit Conversion):** `km/h` se `mph` switch karne par Speedometer, Distance (`km` -> `mi`), aur Average/Max Speed sabhi jagah actual mathematical conversion (`* 0.621371`) hoga (fixing SAFAR 1.0 bug).
5. **FR-05 (Autonomous Crash SOS):** MPU-6050 par $>3.5g$ impact aur $>60°$ tilt 10 second tak rehne par ESP32 15-second warning buzzer bajayega; cancel na hone par `ATD` call aur `AT+CMGS` SMS fire karega.