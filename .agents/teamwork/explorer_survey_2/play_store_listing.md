# Kash: Google Play Store Listing Kit Specifications

---

## 1. Store Metadata

- **App Title**: `Kash: Currency Exchange & Rates` (32 characters / max 50 characters allowed)
- **Short Description**: `Instant, accurate currency conversion for USD, EUR, NIS, JOD & global rates.` (77 characters / max 80 characters allowed)

---

## 2. Full Description (Google Play Formatted Markdown)

```markdown
**Kash** is the modern, high-precision currency exchange application engineered for speed, clarity, and regional financial intelligence. 

Whether you are traveling across the Levant, managing international business transactions, or tracking global market swings, Kash delivers instantaneous multi-currency conversion with zero ads, zero clutter, and full offline reliability.

### ⚡ Why Choose Kash?

• **Instant Conversions As You Type**: Experience fluid, zero-latency rate calculations powered by FreeCurrencyAPI. 
• **Levant & Global Powerhouse**: First-class, real-time rates for **USD (US Dollar)**, **EUR (Euro)**, **NIS / ILS (Israeli New Shekel)**, and **JOD (Jordanian Dinar)**, alongside global currency pairs.
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

## 3. Screenshot Specifications (1080 x 2400 px, 9:16 Aspect Ratio)

### Screenshot 1: Instant Conversion Hero
- **Header Text**: *"Instant Currency Conversion"*
- **Sub-header Text**: *"Real-time calculations the moment you type"*
- **Visual Subject**: Kash home screen showcasing `BaseCurrencySelector` set to **USD ($)**, `QuickAmountInput` showing **$100.00**, and the quick chips (`10`, `50`, `100`, `1000`) with `100` highlighted in glowing emerald.
- **Background**: Deep Charcoal Gradient (`#0B0F17` to `#111827`) with soft emerald ambient blur behind phone frame.

### Screenshot 2: Real-Time Multi-Currency Watchlist
- **Header Text**: *"Levant & Global Focus"*
- **Sub-header Text**: *"Track USD, EUR, NIS, and JOD simultaneously"*
- **Visual Subject**: Crisp `ExchangeRateList` displaying:
  * **NIS / ILS**: `₪ 365.20` (`1 USD = 3.652 NIS` • `+0.42%` green trend pill)
  * **EUR**: `€ 92.15` (`1 USD = 0.921 EUR` • `-0.18%` red trend pill)
  * **JOD**: `د.أ 70.90` (`1 USD = 0.709 JOD` • `0.00%` neutral pill)
- **Background**: Slate gradient with gold & teal subtle lighting highlights.

### Screenshot 3: One-Tap Base Currency Switcher
- **Header Text**: *"Switch Base in Seconds"*
- **Sub-header Text**: *"Over 150+ world currencies at your fingertips"*
- **Visual Subject**: Elegant `ModalBottomSheet` sliding up with clean search bar, recent favorites (USD, EUR, NIS, JOD), and clear currency flag avatars with radio checkmarks.
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

- **Dimensions**: 1024 width x 500 height, PNG or JPEG (no transparency).
- **Background**: Deep slate radial gradient (`#0F172A` center radiating to `#020617` corners) with subtle geometric grid lines (representing financial chart matrices).
- **Focal Center-Left**: Glowing 3D-embossed **Kash Emblem Logo** (Emerald & Mint dynamic arrows with Amber node) at 220x220 px.
- **Focal Center-Right**: Angled, bezel-less smartphone display showcasing the live exchange cards for USD, EUR, NIS, and JOD.
- **Typography**:
  * Title: **KASH** (Bold 54pt, Pure White `#FFFFFF` with slight letter spacing).
  * Tagline: **Currency Exchange & Rates** (Medium 22pt, Mint `#34D399`).
  * Badge: **Levant & Global • Offline Ready** (Amber pill badge `#F59E0B`).

---

## 5. Google Play Store Categorization & Compliance

- **Primary Category**: Finance (`FINANCE`)
- **Secondary Category / Tags**: Currency Converter, Exchange Rates, Travel & Local, Productivity, Levant Finance, Shekel Converter, Dinar Exchange.
- **Content Rating**: Everyone (PEGI 3, ESRB Everyone). Zero violent, vulgar, sexual, or gambling elements.
- **Target Audience**: Travelers, business professionals, international students, digital nomads, and expats.
- **Data Safety Declarations**:
  * *Data Collected*: **None**. No personal information, no identifiers, no financial accounts, no health, no location.
  * *Data Shared*: **None**. Zero third-party analytics SDKs, zero ad networks.
  * *Security Practices*: All network queries to FreeCurrencyAPI use encrypted HTTPS (`TLS 1.3`).
