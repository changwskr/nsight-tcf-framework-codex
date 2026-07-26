$ErrorActionPreference = 'Continue'
$contexts = @('ic','pc','ms','sv','pd','eb','ep','ss','mg','om','ui','jwt','batch')
$base = 'http://localhost:8080'

Write-Host '[ztomcat] Health check (GET /{context}/actuator/health)'
$ok = 0
$fail = 0
foreach ($ctx in $contexts) {
    $url = "$base/$ctx/actuator/health"
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 30
        Write-Host "  OK   $ctx -> $($r.StatusCode)"
        $ok++
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code) {
            Write-Host "  FAIL $ctx -> HTTP $code"
        } else {
            Write-Host "  FAIL $ctx -> $($_.Exception.Message)"
        }
        $fail++
    }
}
Write-Host "[ztomcat] Result: $ok OK, $fail FAIL (total $($contexts.Count))"
if ($fail -gt 0) { exit 1 }
