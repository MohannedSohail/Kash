$rates = @{ 'USD' = 1.0; 'EUR' = 0.92; 'ILS' = 3.65; 'JOD' = 0.7090 }

function Get-Rate($b, $t, $r) {
    if ($b -eq $t) { return 1.0 }
    $uB = if ($b -eq 'USD') { 1.0 } elseif ($b -eq 'JOD') { 0.7090 } else { $r[$b] }
    $uT = if ($t -eq 'USD') { 1.0 } elseif ($t -eq 'JOD') { 0.7090 } else { $r[$t] }
    return $uT / $uB
}

Write-Host "=== 1. JOD-USD PEG VERIFICATION ==="
$usdToJod = Get-Rate 'USD' 'JOD' $rates
$jodToUsd = Get-Rate 'JOD' 'USD' $rates
Write-Host "Rate USD -> JOD: $usdToJod"
Write-Host "Rate JOD -> USD: $jodToUsd"
$c100JodToUsd = 100.0 * $jodToUsd
$c100UsdToJod = 100.0 * $usdToJod
Write-Host ("100 JOD to USD = {0:N2} (exact: {1})" -f $c100JodToUsd, $c100JodToUsd)
Write-Host ("100 USD to JOD = {0:N3} (exact: {1})" -f $c100UsdToJod, $c100UsdToJod)

Write-Host "`n=== 2. CLOSED-LOOP EQUILIBRIUM ALL 24 PERMUTATIONS ==="
$currencies = @('USD', 'EUR', 'ILS', 'JOD')
$permutations = @(
    @('USD', 'EUR', 'ILS', 'JOD'),
    @('USD', 'EUR', 'JOD', 'ILS'),
    @('USD', 'ILS', 'EUR', 'JOD'),
    @('USD', 'ILS', 'JOD', 'EUR'),
    @('USD', 'JOD', 'EUR', 'ILS'),
    @('USD', 'JOD', 'ILS', 'EUR'),
    @('EUR', 'USD', 'ILS', 'JOD'),
    @('EUR', 'USD', 'JOD', 'ILS'),
    @('EUR', 'ILS', 'USD', 'JOD'),
    @('EUR', 'ILS', 'JOD', 'USD'),
    @('EUR', 'JOD', 'USD', 'ILS'),
    @('EUR', 'JOD', 'ILS', 'USD'),
    @('ILS', 'USD', 'EUR', 'JOD'),
    @('ILS', 'USD', 'JOD', 'EUR'),
    @('ILS', 'EUR', 'USD', 'JOD'),
    @('ILS', 'EUR', 'JOD', 'USD'),
    @('ILS', 'JOD', 'USD', 'EUR'),
    @('ILS', 'JOD', 'EUR', 'USD'),
    @('JOD', 'USD', 'EUR', 'ILS'),
    @('JOD', 'USD', 'ILS', 'EUR'),
    @('JOD', 'EUR', 'USD', 'ILS'),
    @('JOD', 'EUR', 'ILS', 'USD'),
    @('JOD', 'ILS', 'USD', 'EUR'),
    @('JOD', 'ILS', 'EUR', 'USD')
)

$maxDrift = 0.0
foreach ($p in $permutations) {
    $c1 = $p[0]; $c2 = $p[1]; $c3 = $p[2]; $c4 = $p[3]
    $r1 = Get-Rate $c1 $c2 $rates
    $r2 = Get-Rate $c2 $c3 $rates
    $r3 = Get-Rate $c3 $c4 $rates
    $r4 = Get-Rate $c4 $c1 $rates
    $loop = $r1 * $r2 * $r3 * $r4
    $diff = [Math]::Abs($loop - 1.0)
    if ($diff -gt $maxDrift) { $maxDrift = $diff }
    Write-Host ("{0} -> {1} -> {2} -> {3} -> {0} = {4:F10} (drift: {5:E3})" -f $c1, $c2, $c3, $c4, $loop, $diff)
}
Write-Host "`nMaximum drift across all 24 permutations: $maxDrift"

Write-Host "`n=== 3. BOUNDARY VALUES & MATHEMATICAL EXTREMES ==="
$amounts = @(0.0, -50.0, 1e-6, 1e12, 1e15)
foreach ($amt in $amounts) {
    $sanitized = if ($amt -le 0.0 -or [Double]::IsNaN($amt) -or [Double]::IsInfinity($amt)) { 0.0 } else { $amt }
    $convertedUsdToJod = $sanitized * $usdToJod
    $convertedJodToUsd = $sanitized * $jodToUsd
    Write-Host ("Input: {0,18} -> Sanitized: {1,18} | JOD: {2,18:N3} | USD: {3,18:N2}" -f $amt, $sanitized, $convertedUsdToJod, $convertedJodToUsd)
}
