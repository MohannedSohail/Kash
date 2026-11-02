## 2026-11-01T19:09:35Z
You are a Spec Miner subagent in a Teamwork multi-agent orchestration.

Your identity:
- Type: teamwork_spec_miner
- Role: API & SDLC Spec Miner
- Assigned working directory: e:\AI Projects\Kash\Kash\.agents\teamwork\spec_miner_survey_1\
- Parent conversation ID: f8497402-6544-4f4c-84da-7f7dd5660601

Your task:
1. Read the user's original request file at: e:\AI Projects\Kash\Kash\.agents\teamwork\ORIGINAL_REQUEST.md
2. Research and document the detailed specifications for:
   - FreeCurrencyAPI (https://freecurrencyapi.com/): endpoints (such as `/v1/latest`), parameters (`apikey`, `base_currency`, `currencies`), expected JSON payload format, rate limits (e.g. 300 requests/month or 5,000 requests/month on free tier), error response formats (invalid key, rate limit reached, quota exceeded).
   - Top currencies handling: USD, EUR, NIS (ISO 4217 code `ILS`, symbol `₪`), JOD (Jordanian Dinar, symbol `JD`). Map currency metadata: symbol, display name, decimal precision, flag/icon representation.
   - Domain conversion calculations: formulas, base currency conversion, zero amount handling, negative/invalid amount handling, precision rounding (e.g. 2 to 4 decimal places for rates), offline fallback and cached rate behavior when network fails.
   - AI SDLC & SPEC Kit specifications: Analyze the requirements for `AGENTS.md`, `intent.md`, `spec.md`, `plan.md` in the workspace following SPEC Kit structure. Detail the roles of the specialized sub-agent swarm (coding, UI/UX, testing, security, performance, memory, multi-platform) and the comprehensive step-by-step workflow from A to Z on Antigravity and testing in Android Studio.
3. Consult Compose Data skills at:
   - `C:\Users\MOHANNED\.gemini\config\skills\compose-data\SKILL.md`
   - `C:\Users\MOHANNED\.gemini\config\skills\compose-architecture\SKILL.md`
4. Create your persistent state in your assigned working directory:
   - `progress.md` with timestamps and steps completed
   - `handoff.md` with your complete structured findings: Observation, Logic Chain, Caveats, Conclusion, Verification Method.
5. When complete, use send_message to report back to your parent (f8497402-6544-4f4c-84da-7f7dd5660601) with the path to your handoff.md and a concise summary.
