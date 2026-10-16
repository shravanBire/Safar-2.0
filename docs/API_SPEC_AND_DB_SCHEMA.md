# SAFAR 2.0 — Wire Protocol Specification & Local Database Schema

## 1. QR Code Pairing Payload Format
Jab user `AddDeviceDialog` me QR scan karega, QR ke andar yeh URI hogi:
`safar://pair?id=SAFAR_01&name=Classic350&pub=<64_HEX_NOSTR_PUBKEY>&sec=<64_HEX_CHACHA20_KEY>&sim=+919876543210`

---

## 2. Binary Wire Protocol (ESP32 <-> Android App)

### A. 16-Byte Packed `TripPoint` Struct (Used in SRAM, LittleFS & Live UDP)
```c
struct __attribute__((packed)) TripPoint {
    uint32_t timestamp;   // Bytes 0..3  : Unix Epoch Seconds (UTC)
    int32_t  lat_e7;      // Bytes 4..7  : Latitude * 10,000,000
    int32_t  lon_e7;      // Bytes 8..11 : Longitude * 10,000,000
    uint16_t speed_decideg; // Bytes 12..13: Speed in km/h * 10
    uint8_t  heading_div2;// Byte 14     : Course angle / 2
    uint8_t  flags;       // Byte 15     : Bit 0=Ignition, Bit 1=Start, Bit 2=End, Bits 3..7=Sats
}; // Total = Exactly 16 Bytes!

Packet Types (1-Byte Header Prefix)
Opcode	Name			    Direction	    PayloadSize 	Description
0x01	PKT_OPEN_SESSION	App -> ESP32	24 Bytes	    App_IPv6 (16B) + Port (2B) + Last_Synced_TS (4B) + CRC16 (2B)
0x02	PKT_LIVE_TELEMETRY	ESP32 -> App	20 Bytes	    Seq_Num (2B) + TripPoint (16B) + CRC16 (2B)
0x03	PKT_HISTORY_CHUNK	ESP32 -> App	488 Bytes	    Chunk_ID (2B) + Total_Chunks (2B) + Count (1B) + 30x TripPoint (480B) + CRC16 (2B)
0x04	PKT_HISTORY_ACK		App -> ESP32	6 Bytes		    Chunk_ID (2B) + Status (1B) + Reserved (1B) + CRC16 (2B)
0x05	PKT_HEARTBEAT		Bidirectional	6 Bytes		    Sent every 25s while App is open to keep UDP NAT pin-hole alive

3. Android Local RoomDB Schema (app/src/main/java/com/example/safar/data/local/)
SAFAR 1.0 ke build.gradle.kts me Room 2.6.1 installed hai par use nahi hua. SAFAR 2.0 me Supabase tables ko hata kar yeh 3 local Room tables banengi:

Table 1: devices (DeviceEntity.kt)
device_id: String (@PrimaryKey)

device_name: String

nostr_pub_key: String

shared_secret_hex: String

sim_number: String

last_synced_timestamp: Long

last_lat: Double?, last_lon: Double?, last_speed: Float?

is_selected: Boolean

Table 2: trip_summaries (TripSummaryEntity.kt)
trip_id: String (@PrimaryKey)

device_id: String

start_timestamp: Long, end_timestamp: Long

distance_km: Double, duration_minutes: Int

avg_speed_kmh: Double, max_speed_kmh: Double

Table 3: location_points (LocationPointEntity.kt)
id: Long (@PrimaryKey(autoGenerate = true))

trip_id: String (Indexed)

device_id: String (Indexed)

timestamp: Long (Indexed)

latitude: Double, longitude: Double

speed_kmh: Float, heading: Int

### 5. TECH_STACK_AND_ADRS.md (Tech Stack, ADRs & Test Plan)

```markdown
# SAFAR 2.0 — Tech Stack, ADRs, Risk Register & Test Plan

## 1. Finalized Tech Stack
### Hardware & Embedded Firmware (ESP32)
- **Framework:** ESP-IDF v5.x / Arduino-ESP32 Core
- **Modem Driver:** `esp_modem` (CMUX + PPPoS Dual-Stack IPv4/IPv6 LwIP)
- **Storage:** `SRAM` 4KB Ring-Buffer + `LittleFS` Partition (2MB dedicated flash partition)
- **Sensors:** `TinyGPSPlus` (NEO-6M parser), `MPU6050_light` / I2C raw register driver
- **Crypto:** `mbedTLS` (ChaCha20-Poly1305) + `micro-ecc` (`secp256k1` Nostr signing)

### Android App (`com.example.safar`)
- **UI & Architecture:** Kotlin, Jetpack Compose (Material3 custom Retro-Moto theme), MVVM + Clean Repository Pattern
- **Local Persistence:** `RoomDB` (Devices, Trips, Points) + `Jetpack DataStore` (Theme & Unit preferences)
- **Networking:** `OkHttp` WebSockets (Nostr Signaling) + Java NIO `DatagramChannel` (IPv6 UDP P2P)
- **Mapping:** `MapLibre Native Android SDK 11.8.0` with custom dark/vintage tile style
- **QR Scanner:** Google ML Kit Barcode Scanning + `CameraX`

---

## 2. Architecture Decision Records (ADRs)
- **ADR-01 (Eliminate Supabase for P2P + Edge Storage):** SAFAR 1.0 me har GPS point Supabase me insert hota tha aur app pure table ko pull karti thi. SAFAR 2.0 me data ESP32 ke `LittleFS` me rehta hai aur app khulne par P2P UDP se `RoomDB` me sync hota hai. Cost = ₹0.
- **ADR-02 (App-Independent GSM SOS):** SOS ke liye internet ya app background service par depend rehna unsafe hai. ESP32 MPU-6050 se crash detect karke seedha `ATD` call aur `AT+CMGS` SMS karega.
- **ADR-03 (4KB SRAM Batching before LittleFS):** Flash wear-out rokne ke liye har 1-second point pehle RAM me jayega aur har 256 points (4.5 min) ya Ignition-OFF par flash me flush hoga.
- **ADR-04 (Consistent Hash Relay Sharding):** 10,000+ devices par rate-limit rokne ke liye `SHA256(PubKey) % 50` se har device apne 3 Nostr relays chunta hai.

---

## 3. Risk Register & Mitigation
| Risk | Impact | Mitigation Built into SAFAR 2.0 |
| :--- | :--- | :--- |
| **False Crash SOS on Potholes** | Rider ko faltu call/SMS jayega | Impact ($>3.5g$) KE SAATH 10-second continuous tilt ($>60°$) + 15-second physical buzzer cancel window mandatory hai. |
| **Strict Wi-Fi Blocks UDP** | P2P hole punch fail hoga | 1.5s timeout ke baad automatically Nostr WebSocket (`Kind 20000`) fallback active ho jayega. |
| **Sudden Bike Battery Disconnect** | RAM buffer ke 4.5 min ke points udd jayenge | Tracker me ek chota `1000mAh LiPo backup battery` rahega jo main 12V cut hote hi SRAM ko `LittleFS` me flush karega. |