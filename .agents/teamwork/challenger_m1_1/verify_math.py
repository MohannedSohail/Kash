"""
verify_math.py - Empirical test harness for Kash currency exchange mathematics
Tests JOD peg derivations, cross-rates, rounding rules, edge cases, and numerical stability.
"""

from decimal import Decimal, ROUND_HALF_UP
import math

JOD_PEG_FLOAT = 0.7090
JOD_PEG_DECIMAL = Decimal("0.7090")

# Sample real-world rates against USD
RATES_USD = {
    "USD": 1.0,
    "EUR": 0.91845,
    "ILS": 3.65420,
    "JOD": 0.7090,
}

RATES_USD_DECIMAL = {
    "USD": Decimal("1.0000"),
    "EUR": Decimal("0.91845"),
    "ILS": Decimal("3.65420"),
    "JOD": Decimal("0.7090"),
}

def format_amount(amount: Decimal, currency: str) -> str:
    decimals = 3 if currency == "JOD" else 2
    pattern = Decimal("1." + "0" * decimals)
    rounded = amount.quantize(pattern, rounding=ROUND_HALF_UP)
    return f"{rounded:,.{decimals}f}"

def format_rate(rate: Decimal) -> str:
    pattern = Decimal("0.0001")
    rounded = rate.quantize(pattern, rounding=ROUND_HALF_UP)
    return f"{rounded:.4f}"

def test_ac2_jod_usd():
    print("=== TEST AC-2: JOD <-> USD CONVERSION ===")
    
    # 100 USD to JOD
    usd_amount = Decimal("100.00")
    jod_converted = usd_amount * JOD_PEG_DECIMAL
    jod_formatted = format_amount(jod_converted, "JOD")
    print(f"100 USD -> JOD: raw={jod_converted}, formatted={jod_formatted}")
    assert jod_formatted == "70.900", f"Expected '70.900', got '{jod_formatted}'"

    # 100 JOD to USD
    jod_amount = Decimal("100.00")
    usd_converted = jod_amount / JOD_PEG_DECIMAL
    usd_formatted_2dec = format_amount(usd_converted, "USD")
    usd_formatted_3dec = format_amount(usd_converted, "JOD")  # if 3 decimals
    print(f"100 JOD -> USD: raw={usd_converted}, 2dec='{usd_formatted_2dec}', 3dec='{usd_formatted_3dec}'")
    
    # Check rate display
    rate_jod_to_usd = Decimal("1.0") / JOD_PEG_DECIMAL
    rate_formatted = format_rate(rate_jod_to_usd)
    print(f"Rate JOD -> USD: raw={rate_jod_to_usd}, formatted={rate_formatted}")
    
    print("AC-2 inspection complete.\n")

def test_cross_rates_all_bases():
    print("=== TEST ALL BASE CURRENCIES (USD, EUR, ILS, JOD) ===")
    currencies = ["USD", "EUR", "ILS", "JOD"]
    
    # Using USD rates as the ground truth
    # If Base is B, rate B -> T = (USD -> T) / (USD -> B)
    for base in currencies:
        print(f"\n--- Base Currency: {base} ---")
        base_rate_usd = RATES_USD_DECIMAL[base]
        for target in currencies:
            target_rate_usd = RATES_USD_DECIMAL[target]
            # Cross-rate
            cross_rate = target_rate_usd / base_rate_usd
            rate_fmt = format_rate(cross_rate)
            # Test $100 conversion
            converted = Decimal("100") * cross_rate
            amt_fmt = format_amount(converted, target)
            print(f"  {base} -> {target}: Rate={rate_fmt} (raw={cross_rate:.6f}), 100 {base} = {amt_fmt} {target}")
            
            # Identity check
            if base == target:
                assert cross_rate == Decimal("1.0"), f"Identity failed for {base}"
                assert amt_fmt in ("100.00", "100.000"), f"Identity amount format failed for {base}"

def test_round_trip_drift():
    print("\n=== TEST ROUND-TRIP DRIFT (A -> B -> A) ===")
    test_amounts = [
        Decimal("0.01"),
        Decimal("1.00"),
        Decimal("10.00"),
        Decimal("50.00"),
        Decimal("100.00"),
        Decimal("1000.00"),
        Decimal("1000000.00"),
        Decimal("1000000000.00"),
        Decimal("1000000000000.00"),  # 10^12 max amount
    ]
    
    pairs = [
        ("USD", "JOD"),
        ("JOD", "USD"),
        ("EUR", "JOD"),
        ("JOD", "EUR"),
        ("ILS", "JOD"),
        ("JOD", "ILS"),
        ("EUR", "ILS"),
    ]
    
    max_drift_pct = Decimal("0")
    
    for c1, c2 in pairs:
        r1 = RATES_USD_DECIMAL[c2] / RATES_USD_DECIMAL[c1]
        r2 = RATES_USD_DECIMAL[c1] / RATES_USD_DECIMAL[c2]
        
        # Exact mathematical inverse: r1 * r2 == 1.0
        product = r1 * r2
        inverse_diff = abs(product - Decimal("1.0"))
        assert inverse_diff < Decimal("1e-20"), f"Inverse rate mismatch for {c1}-{c2}: {product}"
        
        for amt in test_amounts:
            # Full precision round trip
            forward = amt * r1
            back = forward * r2
            diff = abs(back - amt)
            drift_pct = (diff / amt) * 100
            if drift_pct > max_drift_pct:
                max_drift_pct = drift_pct
                
            # Now test with rounded intermediate (simulating display/cache truncation)
            # e.g., if user takes the displayed converted amount and converts it back
            dec1 = 3 if c2 == "JOD" else 2
            fwd_rounded = forward.quantize(Decimal("1." + "0" * dec1), rounding=ROUND_HALF_UP)
            back_from_rounded = fwd_rounded * r2
            dec2 = 3 if c1 == "JOD" else 2
            back_rounded = back_from_rounded.quantize(Decimal("1." + "0" * dec2), rounding=ROUND_HALF_UP)
            display_diff = abs(back_rounded - amt)
            
            # Print any noticeable drift on $100
            if amt == Decimal("100.00"):
                print(f"  {amt} {c1} -> {fwd_rounded} {c2} -> {back_rounded} {c1} (disp diff: {display_diff})")
                
    print(f"\nMax continuous drift percentage across all pairs and amounts: {max_drift_pct:.20f}%")

def test_edge_cases():
    print("\n=== TEST EDGE CASES & BOUNDARIES ===")
    
    # 1. Zero amount
    zero_amt = Decimal("0.0")
    res_zero = zero_amt * RATES_USD_DECIMAL["JOD"]
    fmt_zero_jod = format_amount(res_zero, "JOD")
    fmt_zero_usd = format_amount(res_zero, "USD")
    print(f"Zero amount: JOD={fmt_zero_jod}, USD={fmt_zero_usd}")
    assert fmt_zero_jod == "0.000"
    assert fmt_zero_usd == "0.00"
    
    # 2. Extreme large amount: 10^12
    max_amt = Decimal("1000000000000.00")
    res_max = max_amt * RATES_USD_DECIMAL["JOD"]
    fmt_max = format_amount(res_max, "JOD")
    print(f"10^12 USD -> JOD: {fmt_max}")
    assert fmt_max == "709,000,000,000.000"
    
    # 3. Double precision float vs Decimal comparison
    # In Kotlin, Double is 64-bit IEEE 754. Let's see if Double introduces representation errors.
    double_peg = 0.7090
    double_inv = 1.0 / double_peg
    print(f"Float 64-bit peg: {double_peg}")
    print(f"Float 64-bit inv peg (1/0.7090): {double_inv:.16f}")
    
    # 100 * 0.7090
    d_jod = 100.0 * double_peg
    print(f"100.0 * 0.7090 in Double: {d_jod} (round to 3: {round(d_jod, 3):.3f})")
    
    # 100 / 0.7090
    d_usd = 100.0 * double_inv
    print(f"100.0 * (1/0.7090) in Double: {d_usd:.16f} (round to 2: {round(d_usd, 2):.2f}, round to 3: {round(d_usd, 3):.3f})")

if __name__ == "__main__":
    test_ac2_jod_usd()
    test_cross_rates_all_bases()
    test_round_trip_drift()
    test_edge_cases()
    print("\nAll mathematical tests finished successfully!")
