# Kash: Google Play Store Listing Kit Specifications

---

## 1. Store Metadata & Character Limits

| Field | Content | Character Count | Limit |
|---|---|---|---|
| **App Title** | `Kash: Currency Exchange & Rates` | 31 chars | Max 50 chars |
| **Short Description** | `Instant, accurate currency conversion for USD, EUR, NIS, JOD & global rates.` | 76 chars | Max 80 chars |

---

## 2. Full Description (Google Play Formatted Markdown)

```markdown
**Kash** is the modern, high-precision currency exchange application engineered for speed, clarity, and regional financial intelligence. 

Whether you are traveling across the Levant, managing international business transactions, or tracking global market swings, Kash delivers instantaneous multi-currency conversion with zero ads, zero clutter, and full offline reliability.

### ⚡ Why Choose Kash?

• **Instant Conversions As You Type**: Experience fluid, zero-latency rate calculations powered by FreeCurrencyAPI. 
• **Levant & Global Powerhouse**: First-class, real-time rates for **USD (US Dollar)**, **EUR (Euro)**, **NIS / ILS (Israeli New Shekel)**, and **JOD (Jordanian Dinar)**, alongside global currency pairs.
• **Official Monetary Pegs**: Automatically derives exact rates with the Central Bank of Jordan USD peg (1 USD = 0.7090 JOD).
• **1-Tap Quick Amount Chips**: Jump immediately to common travel and exchange denominations (10, 50, 100, 1000) or tap to clear and enter custom amounts effortlessly.
• **Offline-First Reliability**: Traveling without data or Wi-Fi? Kash automatically stores the latest exchange rates on your device so you are never left guessing prices at customs, markets, or cafes.
• **Modern Fintech Aesthetic**: Designed with an eye-friendly dark slate canvas, vivid emerald indicators, and crisp typography adhering to strict Material 3 design standards.
• **100% Ad-Free & Respectful**: No intrusive banners, no interstitial video ads, and no required accounts. Kash does one thing and does it with absolute excellence.

---

### 🌐 Key Features

• **Base Currency Selector**: Effortlessly switch your primary currency via a sleek modal sheet.
• **Multi-Currency Watchlist**: Monitor multiple conversion rates simultaneously side-by-side.
• **Unit Rates & Trend Badges**: View direct conversion ratios (e.g. 1 USD = 3.65 NIS) and trend metrics at a glance.
• **Smart Pull-to-Refresh & Silent Updates**: Pull down anytime to sync latest live market rates with a subtle pulse animation.
• **Graceful Error Recovery**: If your network drops, your cached rates stay on screen while a friendly retry banner helps you reconnect.

---

### 🔒 Privacy-First Promise

Your financial queries belong to you.
• No login or account creation required.
• No tracking, cookies, or location permissions requested.
• Read-only rate queries with zero personal data transmission.

---

### 💻 Open & Modern Engineering

Built on Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP), Kash brings identical architectural purity and smooth 60fps animations across mobile and desktop devices.

Download **Kash** today and experience currency conversion the way it was meant to be: fast, focused, and beautifully designed.
```

---

## 3. High-Fidelity Screenshot Blueprints (1080 x 2400 px, 9:16 Aspect Ratio)

### Screenshot 1: Instant Conversion Hero
- **Header Text**: *"Instant Currency Conversion"*
- **Sub-header Text**: *"Real-time calculations the moment you type"*
- **Visual Subject**: Kash home screen showcasing `BaseCurrencySelector` set to **USD ($)**, `QuickAmountInput` showing **$100.00**, and the quick chips (`10`, `50`, `100`, `1000`) with `100` highlighted in glowing emerald.
- **Background**: Deep Charcoal Gradient (`#0B0F17` to `#111827`) with soft emerald ambient blur behind phone frame.

### Screenshot 2: Real-Time Multi-Currency Watchlist
- **Header Text**: *"Levant & Global Focus"*
- **Sub-header Text**: *"Track USD, EUR, NIS, and JOD simultaneously"*
- **Visual Subject**: Crisp `ExchangeRateList` displaying:
  * **NIS / ILS**: `₪ 365.20` (`1 USD = 3.6500 NIS` • `+0.42%` green trend pill)
  * **EUR**: `€ 92.00` (`1 USD = 0.9200 EUR` • `-0.18%` red trend pill)
  * **JOD**: `د.أ 70.90` (`1 USD = 0.7090 JOD` • `0.00%` neutral pill)
- **Background**: Slate gradient with gold & teal subtle lighting highlights.

### Screenshot 3: One-Tap Base Currency Switcher
- **Header Text**: *"Switch Base in Seconds"*
- **Sub-header Text**: *"Top world & regional currencies at your fingertips"*
- **Visual Subject**: Elegant `ModalBottomSheet` displaying USD, EUR, ILS, JOD with currency flag avatars and active radio checkmarks.
- **Background**: Smooth frosted glass dim overlay with crisp sheet elevation.

### Screenshot 4: Offline-First Reliability
- **Header Text**: *"Works Without Wi-Fi"*
- **Sub-header Text**: *"Cached rates ensure you are never stranded offline"*
- **Visual Subject**: Kash running smoothly in airplane mode with a sleek, subtle badge: *"Cached: Today 14:30 • All rates ready"*.
- **Background**: Subtle deep indigo/slate atmosphere conveying rock-solid reliability.

### Screenshot 5: Modern Dark Fintech Aesthetic
- **Header Text**: *"Crafted for Pure Focus"*
- **Sub-header Text**: *"Zero ads. Zero tracking. Just financial clarity."*
- **Visual Subject**: Macro close-up on the typographic hierarchy: bold high-contrast numbers, emerald accents, and crisp 1dp outline borders.
- **Background**: Deep slate with emerald-to-mint luminous accents.

### Screenshot 6: Desktop & Multiplatform Fluidity
- **Header Text**: *"Seamless Multiplatform Power"*
- **Sub-header Text**: *"Consistent experience on phone, tablet, and desktop"*
- **Visual Subject**: Android phone frame tilted alongside a sleek Desktop window layout running the CMP desktop application.
- **Background**: Modern dual-device promotional studio stage.

---

## 4. Feature Graphic Specification (1024 x 500 px)

- **Canvas Dimensions**: 1024px width by 500px height.
- **Color Gradients**: Dark Charcoal base (`#0B0F17`) to Slate (`#1E293B`) with ambient Emerald illumination (`#059669` / `#34D399`).
- **Foreground Composition**:
  - Center-Left: Scaled 256x256 Kash Dynamic Flux Emblem with luminous drop shadow.
  - Center-Right: Bold headline *"Kash"* in 72sp SemiBold White, followed by *"Currency Exchange & Rates"* in 28sp Emerald Mint.
  - Bottom-Right Accent: Floating micro-badges for `$ USD`, `€ EUR`, `₪ NIS`, `د.أ JOD`.

---

## 5. Categorization, Rating & Privacy Disclosures

- **Primary Category**: Finance / Currency Converter
- **Content Rating**: Everyone (PEGI 3, ESRB Everyone)
- **Data Safety Disclosures**:
  - *Data Collected*: None (Zero data collected or shared).
  - *Data Transferred*: Encrypted HTTPS queries to FreeCurrencyAPI for rate lookups.
  - *Permissions*: Minimal network access (`android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`). No location, contacts, or storage permissions required.
