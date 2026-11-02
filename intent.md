# intent.md — Kash Product Intent & Value Proposition

> **Application**: Kash — Modern Compose Multiplatform Currency Exchange  
> **Author**: AI SDLC Product Management & Specification Team  
> **Date**: 2026-11-01  
> **Status**: Approved & Baseline for SPEC Kit  

---

## 1. Problem Statement

In today's interconnected global economy, cross-border travelers, digital nomads, freelancers, and businesses constantly need to convert currency values between major fiat denominations: **US Dollar (USD)**, **Euro (EUR)**, **Israeli New Shekel (NIS / ILS)**, and **Jordanian Dinar (JOD)**.

However, existing currency converter applications suffer from critical friction points:
1. **Bloated & Ad-Ridden Interfaces**: Slow startup times, aggressive full-screen advertisements, and distracting clutter when users need instant answers at a payment counter or airport transit gate.
2. **Fragile Network Dependency**: Crashing or displaying empty white screens when internet connectivity is intermittent (e.g., roaming abroad, subterranean subways, airplane mode).
3. **Regional Currency Gaps**: Many free exchange rate APIs (such as FreeCurrencyAPI's free tier) omit regional currencies like the Jordanian Dinar (JOD) or trigger unhandled HTTP 422 errors, leaving regional users stranded.
4. **Platform Inconsistency**: Disconnected user experiences across mobile phones and desktop workstations, forcing users to switch tools between mobile tasks and desktop financial accounting.

**Kash** resolves these deficiencies by providing a lightweight, blisteringly fast, offline-first Compose Multiplatform application designed with financial elegance, mathematical precision, and zero visual friction.

---

## 2. Target Platforms & Distribution Scope

Kash is engineered natively using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)** to target:

1. **Android**:
   - Primary mobile target supporting smartphones and foldables (Android 7.0+ / API 24 through Android 15 / API 35+).
   - Touch-optimized gesture controls, adaptive launcher icons, Material 3 dynamic color compliance, and haptic-responsive quick amounts.
2. **Desktop JVM (Windows, macOS, Linux)**:
   - Workstation and laptop target for digital nomads, traders, and accountants.
   - Clean 400x720 window profile, keyboard numeric navigation, responsive desktop window scaling, and font-independent currency badge rendering.
3. **iOS (Multiplatform Ready)**:
   - Structured with static framework binding (`Shared.framework`), enabling seamless native embedding in SwiftUI / Xcode hosts.

---

## 3. User Personas

### Persona A: Tariq — The Regional Cross-Border Merchant & Traveler
- **Profile**: 34, trader frequently commuting between Amman (Jordan), Jerusalem/Tel Aviv, and European trade hubs.
- **Pain Point**: Constantly converts between JOD, USD, EUR, and NIS. Mainstream apps fail to support JOD or require paid subscriptions.
- **Needs in Kash**: Rapid switching between JOD and NIS as base currencies; instantaneous calculation; offline rate availability when roaming without data.

### Persona B: Elena — The Digital Nomad & Remote Software Contractor
- **Profile**: 28, freelance designer earning in USD and EUR, living across multiple destinations.
- **Pain Point**: Needs to quickly check how much an invoice in USD or EUR translates to local expenses, often working from laptops and mobile phones simultaneously.
- **Needs in Kash**: Consistent desktop and mobile experience; quick-tap amount chips ($10, $50, $100, $1,000) for rapid estimation; trustworthy rate timestamps.

### Persona C: David — The Budget-Conscious International Tourist
- **Profile**: 45, traveling abroad with family with limited cellular data connectivity.
- **Pain Point**: High data roaming fees make live web lookups expensive; fearful of getting ripped off at local currency exchange kiosks.
- **Needs in Kash**: Reliable offline caching with clear "last updated" timestamps; zero-ad distraction; clear 2-to-3 decimal precision so every cent is accounted for.

---

## 4. Core Value Proposition

| Pillar | How Kash Delivers | User Benefit |
|---|---|---|
| **Speed & Instant Calculation** | Reactive MVI unidirectional state flow. Converted amounts update immediately as the user types, debounced smoothly at 60fps. | Zero waiting; answers appear as fast as fingers move. |
| **Complete Top 4 Currency Coverage** | Direct FreeCurrencyAPI integration for USD, EUR, and ILS, coupled with official Central Bank of Jordan USD peg synthesis ($1 \text{ USD} = 0.7090 \text{ JOD}$). | Complete coverage of USD, EUR, NIS, and JOD without API 422 errors or paywalls. |
| **Offline-First Resilience** | Cache-aside architecture using Preferences DataStore with bundled seed rates for cold starts. Non-destructive refresh states. | App never crashes or shows blank screens, even in airplane mode. |
| **Fintech Aesthetics & Trust** | Material 3 Emerald/Mint palette with dark charcoal surfaces, tabular typography, smooth `AnimatedContent` number transitions, and vector branding. | Inspires immediate financial confidence and clarity in high-pressure payment environments. |
| **Unified Cross-Platform Experience** | Single shared Kotlin/Compose codebase powering both Android and Desktop JVM. | Identical, predictable behavior on phone and workstation. |

---

## 5. Core UX Philosophy & Design Principles

1. **Information Density with Cognitive Calm**:
   - The primary screen focuses strictly on the active conversion: base currency selector, amount input, and immediate live rate cards for all target currencies. No navigation labyrinths or secondary distractions.
2. **Never Blank During Refresh (Non-Destructive UX)**:
   - During a background refresh or pull-to-refresh, current rates remain fully visible and usable. Skeletons are reserved strictly for the initial cold start when no cache exists.
3. **Tabular Precision & Zero Layout Shift**:
   - Numbers are displayed in tabular font configurations with fixed decimal precision (2 decimals for USD, EUR, ILS; 3 decimals for JOD). As amounts grow, layout dimensions remain rock-solid without jarring element jumps.
4. **Sensible Fallbacks Over Technical Errors**:
   - When a network rate limit or disconnection occurs, the user is never confronted with an unhandled exception or cryptic stack trace. Instead, a polite banner notifies: *"Showing cached rates from [Time]"*, keeping the user empowered.

---

## 6. Success Metrics & Key Results (OKRs)

- **Performance**: Cold app startup to interactive UI $< 500\text{ ms}$ on mobile devices; zero dropped frames ($60\text{ fps}$ / $120\text{ fps}$) during amount typing.
- **Reliability**: $0\%$ crash rate on network outages or invalid currency queries; $100\%$ uptime powered by DataStore caching and seed rate fallbacks.
- **Precision**: Exact mathematical parity with Central Bank official exchange formulas ($HALF\_UP$ rounding, exact 4-decimal exchange rates).
- **Usability**: Single-tap base currency switching; quick amount selection reachable in $\le 1$ second.
