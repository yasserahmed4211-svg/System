# Root cause probes
$ErrorActionPreference = "Continue"
$ApiBase = "http://localhost:7272"
$loginR = Invoke-RestMethod -Uri "$ApiBase/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$Token = $loginR.data.accessToken
$H = @{ "Authorization" = "Bearer $Token"; "Content-Type" = "application/json" }

function Probe($Label, $Method, $Uri, $Body) {
  Write-Host "`n=== $Label ==="
  try {
    $p = @{ Uri=$Uri; Method=$Method; Headers=$H; ErrorAction="Stop" }
    if ($Body) { $p.Body = $Body }
    $r = Invoke-WebRequest @p
    Write-Host "HTTP $($r.StatusCode)"
    Write-Host ($r.Content | ConvertFrom-Json | ConvertTo-Json -Depth 4)
  } catch {
    $sc = try { $_.Exception.Response.StatusCode.value__ } catch { 0 }
    $bt = ""
    try {
      $st2 = $_.Exception.Response.GetResponseStream()
      $rd  = New-Object System.IO.StreamReader($st2)
      $bt  = $rd.ReadToEnd()
    } catch {}
    Write-Host "HTTP $sc"
    Write-Host $bt
  }
}

# 1. Consumption endpoint 500
Probe "1. Consumption CURRENCY" GET "$ApiBase/api/lookups/CURRENCY"

# 2. isActive filter 500
Probe "2. isActive search filter" POST "$ApiBase/api/masterdata/master-lookups/search" '{"page":0,"size":5,"filters":[{"field":"isActive","operator":"EQUALS","value":"true"}]}'

# 3. Detail create with fresh code under latest lookup
$search = (Invoke-WebRequest -Uri "$ApiBase/api/masterdata/master-lookups/search" -Method POST -Headers $H -Body '{"page":0,"size":3,"sorts":[{"field":"createdAt","direction":"DESC"}]}').Content | ConvertFrom-Json
$latestId  = $search.data.content[0].id
$latestKey = $search.data.content[0].lookupKey
Write-Host "`n=== Latest lookup: id=$latestId key=$latestKey ==="
$body3 = "{`"masterLookupId`":$latestId,`"code`":`"CODE_TEST_PROBE_999`",`"nameAr`":`"مسبار`"}"
Probe "3. Detail create fresh code" POST "$ApiBase/api/masterdata/master-lookups/details" $body3

# 4. Search for CODE_01 globally
Probe "4. Does CODE_01 exist?" POST "$ApiBase/api/masterdata/master-lookups/details/search" '{"page":0,"size":5,"filters":[{"field":"code","operator":"EQUALS","value":"CODE_01"}]}'

# 5. Invalid sort field 422 body
Probe "5. Invalid sort field 422" POST "$ApiBase/api/masterdata/master-lookups/search" '{"page":0,"size":5,"sorts":[{"field":"nonExistent","direction":"ASC"}]}'
