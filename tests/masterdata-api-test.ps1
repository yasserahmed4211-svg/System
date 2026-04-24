# ============================================================
# Masterdata Module — Full API Test Runner (PS 5.1 compatible)
# Usage: powershell -ExecutionPolicy Bypass -File tests\masterdata-api-test.ps1
# ============================================================

$ErrorActionPreference = "SilentlyContinue"
$ApiBase = "http://localhost:7272"
$LOOKUPS  = "$ApiBase/api/masterdata/master-lookups"
$DETAILS  = "$ApiBase/api/masterdata/master-lookups/details"
$CONSUMPTION = "$ApiBase/api/lookups"
$UID = "TC$(Get-Date -Format 'HHmmss')"

# ---- Get token ----
$loginR = Invoke-RestMethod -Uri "$ApiBase/api/auth/login" -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
$Token = $loginR.data.accessToken
$H = @{ "Authorization" = "Bearer $Token"; "Content-Type" = "application/json" }

$results = New-Object System.Collections.ArrayList

function Rec($Id, $Group, $Pass, $Expected, $Actual, $Notes) {
  $st = if ($Pass) { "PASS" } else { "FAIL" }
  $icon = if ($Pass) { "[PASS]" } else { "[FAIL]" }
  $null = $results.Add([PSCustomObject]@{
    tcId=$Id; group=$Group; status=$st; expected=$Expected; actual=$Actual; notes=$Notes
  })
  Write-Host "$icon $Id | $Notes"
  if (-not $Pass) {
    Write-Host "       Expected : $Expected"
    Write-Host "       Actual   : $Actual"
  }
}

function Call($Method, $Uri, $Body) {
  try {
    $p = @{ Uri=$Uri; Method=$Method; Headers=$H; ErrorAction="Stop" }
    if ($Body) { $p.Body = $Body }
    $resp = Invoke-WebRequest @p
    return @{ status=[int]$resp.StatusCode; body=($resp.Content | ConvertFrom-Json) }
  } catch {
    $sc = try { $_.Exception.Response.StatusCode.value__ } catch { 0 }
    $bt = ""
    try {
      $st2 = $_.Exception.Response.GetResponseStream()
      $rd  = New-Object System.IO.StreamReader($st2)
      $bt  = $rd.ReadToEnd()
    } catch {}
    $bo = if ($bt) { try { $bt | ConvertFrom-Json } catch { $null } } else { $null }
    return @{ status=$sc; body=$bo }
  }
}

Write-Host ""
Write-Host "=========================================="
Write-Host "  MASTERDATA API TESTS  UID=$UID"
Write-Host "  API Base : $ApiBase"
Write-Host "=========================================="

# ------------------------------------------------------------------
# G1 - Master Lookup Create
# ------------------------------------------------------------------
Write-Host "`n--- G1: Master Lookup Create ---"

$b = "{`"lookupKey`":`"${UID}_VALID`",`"lookupName`":`"اختبار صالح`",`"lookupNameEn`":`"Valid Test`",`"description`":`"desc`",`"isActive`":true}"
$r = Call POST $LOOKUPS $b
$CREATED_ID  = $r.body.data.id
$CREATED_KEY = $r.body.data.lookupKey
$p = ($r.status -eq 201 -and $r.body.success -eq $true -and $CREATED_KEY -eq "${UID}_VALID")
Rec "TC-G1-01" "G1" $p "201 created, key=$CREATED_KEY" "status=$($r.status) id=$CREATED_ID key=$CREATED_KEY" "Valid create all fields"

$r2 = Call POST $LOOKUPS "{`"lookupKey`":`"${UID}_VALID`",`"lookupName`":`"Dup`"}"
$errCode2 = $r2.body.error.code
$p2 = ($r2.status -eq 409 -and $r2.body.success -eq $false)
Rec "TC-G1-02" "G1" $p2 "409, errorCode=MASTER_LOOKUP_KEY_DUPLICATE" "status=$($r2.status) errCode=$errCode2" "Duplicate lookupKey -> 409"

$r3 = Call POST $LOOKUPS "{`"lookupKey`":`"${UID}_NONAME`"}"
Rec "TC-G1-03" "G1" ($r3.status -eq 400) "400 validation" "status=$($r3.status)" "Missing required lookupName -> 400"

$r4 = Call POST $LOOKUPS "{`"lookupKey`":`"${UID}_lower`",`"lookupName`":`"Lower Test`"}"
$actualKey4 = $r4.body.data.lookupKey
$LOWER_ID    = $r4.body.data.id
Rec "TC-G1-04" "G1" ($r4.status -eq 201 -and $actualKey4 -eq "${UID}_LOWER") "201 key normalized to UPPER" "status=$($r4.status) key=$actualKey4" "Lowercase key normalization"

$longKey = "A" * 51
$r5 = Call POST $LOOKUPS "{`"lookupKey`":`"$longKey`",`"lookupName`":`"Too Long`"}"
Rec "TC-G1-05" "G1" ($r5.status -eq 400) "400 @Size(max=50)" "status=$($r5.status)" "lookupKey max 50 chars"

$r6 = Call POST $LOOKUPS "{`"lookupKey`":`"${UID}_NOACT`",`"lookupName`":`"No Active`"}"
$NOACT_ID = $r6.body.data.id
Rec "TC-G1-06" "G1" ($r6.status -eq 201) "201 isActive defaults" "status=$($r6.status) isActive=$($r6.body.data.isActive)" "isActive default behavior"

$r7 = Call POST $LOOKUPS "{`"lookupKey`":`"${UID}_INACT`",`"lookupName`":`"Inactive`",`"isActive`":false}"
$INACT_ID = $r7.body.data.id
Rec "TC-G1-07" "G1" ($r7.status -eq 201 -and $r7.body.data.isActive -eq $false) "201 isActive=false" "status=$($r7.status) isActive=$($r7.body.data.isActive)" "Create with isActive=false"

# ------------------------------------------------------------------
# G2 - Master Lookup Read
# ------------------------------------------------------------------
Write-Host "`n--- G2: Master Lookup Read ---"

$r = Call GET "$LOOKUPS/$CREATED_ID"
Rec "TC-G2-01" "G2" ($r.status -eq 200 -and $r.body.data.id -eq $CREATED_ID) "200 with id" "status=$($r.status) id=$($r.body.data.id)" "Get by valid ID"

$r2 = Call GET "$LOOKUPS/999999"
Rec "TC-G2-02" "G2" ($r2.status -eq 404) "404" "status=$($r2.status)" "Get by non-existent ID"

$r3 = Call GET "$LOOKUPS/$CREATED_ID"
$dcPresent = $r3.body.data.PSObject.Properties.Name -contains "detailCount"
Rec "TC-G2-03" "G2" $dcPresent "detailCount field in response" "detailCount=$($r3.body.data.detailCount) present=$dcPresent" "detailCount @Formula field"

$d4 = $r3.body.data
$hasCA = ($d4.PSObject.Properties.Name -contains "createdAt") -and ($d4.createdAt -ne $null)
$hasCB = ($d4.PSObject.Properties.Name -contains "createdBy") -and ($d4.createdBy -ne $null)
Rec "TC-G2-04" "G2" ($hasCA -and $hasCB) "createdAt and createdBy present" "createdAt=$($d4.createdAt) createdBy=$($d4.createdBy)" "Audit fields in GET response"

# ------------------------------------------------------------------
# G3 - Master Lookup Search
# ------------------------------------------------------------------
Write-Host "`n--- G3: Master Lookup Search ---"

$r = Call POST "$LOOKUPS/search" '{"page":0,"size":10}'
Rec "TC-G3-01" "G3" ($r.status -eq 200 -and $r.body.data.content -ne $null) "200 paginated" "status=$($r.status) total=$($r.body.data.totalElements)" "Search all no filters"

$sb2 = "{`"page`":0,`"size`":10,`"filters`":[{`"field`":`"lookupKey`",`"operator`":`"EQUALS`",`"value`":`"${UID}_VALID`"}]}"
$r2 = Call POST "$LOOKUPS/search" $sb2
Rec "TC-G3-02" "G3" ($r2.status -eq 200 -and $r2.body.data.totalElements -eq 1) "200 totalElements=1" "status=$($r2.status) total=$($r2.body.data.totalElements)" "Filter EQUALS lookupKey"

$r3 = Call POST "$LOOKUPS/search" '{"page":0,"size":50,"filters":[{"field":"isActive","operator":"EQUALS","value":"true"}]}'
Rec "TC-G3-03" "G3" ($r3.status -eq 200) "200 filtered active" "status=$($r3.status) total=$($r3.body.data.totalElements)" "Filter isActive=true"

$sb4 = "{`"page`":0,`"size`":20,`"filters`":[{`"field`":`"lookupKey`",`"operator`":`"CONTAINS`",`"value`":`"$UID`"}]}"
$r4 = Call POST "$LOOKUPS/search" $sb4
Rec "TC-G3-04" "G3" ($r4.status -eq 200 -and $r4.body.data.totalElements -ge 3) ">=3 results" "status=$($r4.status) total=$($r4.body.data.totalElements)" "CONTAINS filter"

$r5 = Call POST "$LOOKUPS/search" '{"page":0,"size":10,"sorts":[{"field":"nonExistent","direction":"ASC"}]}'
Rec "TC-G3-05" "G3" ($r5.status -eq 400 -or $r5.status -eq 200) "400 or graceful" "status=$($r5.status)" "Invalid sort field"

$r6 = Call POST "$LOOKUPS/search" '{"page":0,"size":2}'
$respSz = if ($r6.body.data.content) { $r6.body.data.content.Count } else { 0 }
Rec "TC-G3-06" "G3" ($r6.status -eq 200 -and $respSz -le 2) "200 page size <=2" "status=$($r6.status) returned=$respSz" "Pagination size respected"

# ------------------------------------------------------------------
# G4 - Master Lookup Update
# ------------------------------------------------------------------
Write-Host "`n--- G4: Master Lookup Update ---"

$r = Call PUT "$LOOKUPS/$CREATED_ID" '{"lookupName":"Updated Name","lookupNameEn":"Updated EN","description":"New desc"}'
Rec "TC-G4-01" "G4" ($r.status -eq 200 -and $r.body.data.lookupName -eq "Updated Name") "200 name updated" "status=$($r.status) name=$($r.body.data.lookupName)" "Update name fields"

$r2 = Call GET "$LOOKUPS/$CREATED_ID"
Rec "TC-G4-02" "G4" ($r2.body.data.lookupKey -eq "${UID}_VALID") "key unchanged" "key=$($r2.body.data.lookupKey)" "lookupKey immutability"

$updAt = $r.body.data.updatedAt
Rec "TC-G4-03" "G4" ($updAt -ne $null) "updatedAt populated" "updatedAt=$updAt" "updatedAt set after update"

$r4 = Call PUT "$LOOKUPS/999999" '{"lookupName":"Ghost"}'
Rec "TC-G4-04" "G4" ($r4.status -eq 404) "404" "status=$($r4.status)" "Update non-existent ID"

# ------------------------------------------------------------------
# G5 - Toggle Active (Lookup)
# ------------------------------------------------------------------
Write-Host "`n--- G5: Toggle Active (Lookup) ---"

$r = Call PUT "$LOOKUPS/$CREATED_ID/toggle-active" '{"active":false}'
Rec "TC-G5-01" "G5" ($r.status -eq 200 -and $r.body.data.isActive -eq $false) "200 deactivated" "status=$($r.status) isActive=$($r.body.data.isActive)" "Deactivate lookup no active details"

$r2 = Call PUT "$LOOKUPS/$CREATED_ID/toggle-active" '{"active":true}'
Rec "TC-G5-02" "G5" ($r2.status -eq 200 -and $r2.body.data.isActive -eq $true) "200 reactivated" "status=$($r2.status) isActive=$($r2.body.data.isActive)" "Re-activate lookup"

# ------------------------------------------------------------------
# G6 - Usage (Lookup)
# ------------------------------------------------------------------
Write-Host "`n--- G6: Lookup Usage ---"

$r = Call GET "$LOOKUPS/$CREATED_ID/usage"
Rec "TC-G6-01" "G6" ($r.status -eq 200 -and $r.body.data.totalDetails -eq 0 -and $r.body.data.canDelete -eq $true) "200 totalDetails=0 canDelete=true" "status=$($r.status) total=$($r.body.data.totalDetails) canDelete=$($r.body.data.canDelete)" "Usage no details"

$hasCanDeact = $r.body.data.PSObject.Properties.Name -contains "canDeactivate"
Rec "TC-G6-02" "G6" $hasCanDeact "canDeactivate field present" "canDeactivate=$($r.body.data.canDeactivate)" "canDeactivate field in usage"

# ------------------------------------------------------------------
# G7 - Delete Lookup
# ------------------------------------------------------------------
Write-Host "`n--- G7: Delete Lookup ---"

$r = Call DELETE "$LOOKUPS/$NOACT_ID"
Rec "TC-G7-01" "G7" ($r.status -eq 204 -or $r.status -eq 200) "204 deleted" "status=$($r.status)" "Delete with no details"

$r2 = Call GET "$LOOKUPS/$NOACT_ID"
Rec "TC-G7-02" "G7" ($r2.status -eq 404) "404 after delete" "status=$($r2.status)" "Verify deletion"

# ------------------------------------------------------------------
# G8 - Lookup Detail Create
# ------------------------------------------------------------------
Write-Host "`n--- G8: Lookup Detail Create ---"

$b1 = "{`"masterLookupId`":$CREATED_ID,`"code`":`"CODE_01`",`"nameAr`":`"التفصيل الأول`",`"nameEn`":`"First`",`"sortOrder`":1,`"isActive`":true}"
$r = Call POST $DETAILS $b1
$DETAIL_ID = $r.body.data.id
Rec "TC-G8-01" "G8" ($r.status -eq 201 -and $r.body.data.code -eq "CODE_01") "201 detail created" "status=$($r.status) id=$DETAIL_ID code=$($r.body.data.code)" "Valid detail create"

$r2 = Call POST $DETAILS "{`"masterLookupId`":$CREATED_ID,`"code`":`"CODE_01`",`"nameAr`":`"مكرر`"}"
$err2 = $r2.body.error.code
Rec "TC-G8-02" "G8" ($r2.status -eq 409) "409 duplicate code" "status=$($r2.status) err=$err2" "Duplicate code same master"

$r3 = Call POST $DETAILS '{"masterLookupId":999999,"code":"GHOST","nameAr":"غير موجود"}'
Rec "TC-G8-03" "G8" ($r3.status -eq 404 -or $r3.status -eq 400) "404/400" "status=$($r3.status)" "Non-existent masterLookupId"

$r4 = Call POST $DETAILS "{`"masterLookupId`":$CREATED_ID,`"code`":`"CODE_NONAME`"}"
Rec "TC-G8-04" "G8" ($r4.status -eq 400) "400 validation" "status=$($r4.status)" "Missing required nameAr"

$r5 = Call POST $DETAILS "{`"masterLookupId`":$CREATED_ID,`"code`":`"CODE_02`",`"nameAr`":`"بدون ترتيب`"}"
$DETAIL_ID2 = $r5.body.data.id
Rec "TC-G8-05" "G8" ($r5.status -eq 201 -and $r5.body.data.sortOrder -eq 0) "201 sortOrder=0" "status=$($r5.status) sortOrder=$($r5.body.data.sortOrder)" "sortOrder default 0"

$r6 = Call POST $DETAILS "{`"masterLookupId`":$LOWER_ID,`"code`":`"CODE_01`",`"nameAr`":`"نفس الكود لماستر آخر`"}"
Rec "TC-G8-06" "G8" ($r6.status -eq 201) "201 same code different master" "status=$($r6.status)" "Code uniqueness per master"

$r7 = Call POST $DETAILS "{`"masterLookupId`":$CREATED_ID,`"code`":`"CODE_INACT`",`"nameAr`":`"غير نشط`",`"isActive`":false}"
$DETAIL_INACT_ID = $r7.body.data.id
Rec "TC-G8-07" "G8" ($r7.status -eq 201 -and $r7.body.data.isActive -eq $false) "201 isActive=false" "status=$($r7.status) isActive=$($r7.body.data.isActive)" "Create detail isActive=false"

# ------------------------------------------------------------------
# G9 - Lookup Detail Read & Search
# ------------------------------------------------------------------
Write-Host "`n--- G9: Lookup Detail Read & Search ---"

$r = Call GET "$DETAILS/$DETAIL_ID"
Rec "TC-G9-01" "G9" ($r.status -eq 200 -and $r.body.data.id -eq $DETAIL_ID) "200 detail" "status=$($r.status) id=$($r.body.data.id)" "Get detail by ID"

$r2 = Call GET "$DETAILS/999999"
Rec "TC-G9-02" "G9" ($r2.status -eq 404) "404" "status=$($r2.status)" "Get non-existent detail"

$sb3 = "{`"page`":0,`"size`":20,`"filters`":[{`"field`":`"masterLookupId`",`"operator`":`"EQUALS`",`"value`":`"$CREATED_ID`"}]}"
$r3 = Call POST "$DETAILS/search" $sb3
Rec "TC-G9-03" "G9" ($r3.status -eq 200 -and $r3.body.data.totalElements -ge 2) ">=2 details" "status=$($r3.status) total=$($r3.body.data.totalElements)" "Search by masterLookupId"

$content4 = $r3.body.data.content
$isSorted = $true
if ($content4 -and $content4.Count -gt 1) {
  $orders = $content4 | ForEach-Object { $_.sortOrder }
  for ($i = 1; $i -lt $orders.Count; $i++) {
    if ($orders[$i] -lt $orders[$i-1]) { $isSorted = $false; break }
  }
}
Rec "TC-G9-04" "G9" $isSorted "sortOrder ASC" "isSorted=$isSorted" "Default sort sortOrder ASC"

$r5 = Call GET "$DETAILS/options/${UID}_VALID"
Rec "TC-G9-05" "G9" ($r5.status -eq 200) "200 options" "status=$($r5.status) count=$($r5.body.data.Count)" "Options endpoint by lookupKey"

$hasInact = $false
if ($r5.body.data) {
  $found = $r5.body.data | Where-Object { $_.code -eq "CODE_INACT" }
  $hasInact = ($found -ne $null -and $found.Count -gt 0)
}
Rec "TC-G9-06" "G9" (-not $hasInact) "inactive detail excluded from options" "hasInactiveInOptions=$hasInact" "Options exclude inactive details"

$r7 = Call GET "$DETAILS/$DETAIL_ID/usage"
Rec "TC-G9-07" "G9" ($r7.status -eq 200) "200 usage" "status=$($r7.status) canBeDeleted=$($r7.body.data.canBeDeleted)" "Detail usage endpoint"

# ------------------------------------------------------------------
# G10 - Lookup Detail Update & Toggle
# ------------------------------------------------------------------
Write-Host "`n--- G10: Lookup Detail Update & Toggle ---"

$r = Call PUT "$DETAILS/$DETAIL_ID" '{"nameAr":"الاسم المحدث","nameEn":"Updated Name","sortOrder":5}'
Rec "TC-G10-01" "G10" ($r.status -eq 200 -and $r.body.data.nameAr -eq "الاسم المحدث") "200 nameAr updated" "status=$($r.status) nameAr=$($r.body.data.nameAr)" "Update detail fields"

$r2 = Call GET "$DETAILS/$DETAIL_ID"
Rec "TC-G10-02" "G10" ($r2.body.data.code -eq "CODE_01") "code=CODE_01 unchanged" "code=$($r2.body.data.code)" "Detail code immutability"

Rec "TC-G10-03" "G10" ($r2.body.data.masterLookupId -eq $CREATED_ID) "masterLookupId unchanged" "masterLookupId=$($r2.body.data.masterLookupId)" "Detail masterLookupId immutability"

Rec "TC-G10-04" "G10" ($r.body.data.updatedAt -ne $null) "updatedAt present" "updatedAt=$($r.body.data.updatedAt)" "Detail updatedAt set after update"

$r5 = Call PUT "$DETAILS/$DETAIL_ID/toggle-active" '{"active":false}'
Rec "TC-G10-05" "G10" ($r5.status -eq 200 -and $r5.body.data.isActive -eq $false) "200 detail deactivated" "status=$($r5.status) isActive=$($r5.body.data.isActive)" "Toggle detail inactive"

# Now CREATED_ID still has CODE_02 (active) — deactivate should be blocked
$r6 = Call PUT "$LOOKUPS/$CREATED_ID/toggle-active" '{"active":false}'
$err6 = $r6.body.error.code
Rec "TC-G10-06" "G10" ($r6.status -eq 409 -or $r6.status -eq 400) "409/400 blocked active details" "status=$($r6.status) err=$err6" "Deactivate lookup blocked by active details"

$r7 = Call DELETE "$DETAILS/$DETAIL_ID2"
Rec "TC-G10-07" "G10" ($r7.status -eq 204 -or $r7.status -eq 200) "204 deleted" "status=$($r7.status)" "Delete detail"

$r8 = Call DELETE "$LOOKUPS/$CREATED_ID"
$err8 = $r8.body.error.code
Rec "TC-G10-08" "G10" ($r8.status -eq 409) "409 blocked details exist" "status=$($r8.status) err=$err8" "Delete lookup with details blocked"

$r9 = Call PUT "$DETAILS/999999" '{"nameAr":"Ghost"}'
Rec "TC-G10-09" "G10" ($r9.status -eq 404) "404" "status=$($r9.status)" "Update non-existent detail"

# ------------------------------------------------------------------
# G11 - Lookup Consumption
# ------------------------------------------------------------------
Write-Host "`n--- G11: Lookup Consumption ---"

$r = Call GET "$CONSUMPTION/CURRENCY"
Rec "TC-G11-01" "G11" ($r.status -eq 200 -and $r.body.data.Count -gt 0) "200 list of values" "status=$($r.status) count=$($r.body.data.Count)" "Get seeded CURRENCY values"

$r2 = Call GET "$CONSUMPTION/currency"
Rec "TC-G11-02" "G11" ($r2.status -eq 200 -and $r2.body.data.Count -gt 0) "200 same result as uppercase" "status=$($r2.status) count=$($r2.body.data.Count)" "Lowercase key normalization"

# No auth test
$noAuthH = @{ "Content-Type" = "application/json" }
try {
  $r3n = Invoke-WebRequest -Uri "$CONSUMPTION/CURRENCY" -Method GET -Headers $noAuthH -ErrorAction Stop
  # If it succeeds with 200 -> SECURITY ISSUE (no @PreAuthorize)
  Rec "TC-G11-03" "G11" $false "401 unauthorized" "OPEN ENDPOINT status=$($r3n.StatusCode) — registry confirms no @PreAuthorize" "SECURITY: Consumption endpoint open without auth"
} catch {
  $sc3 = try { $_.Exception.Response.StatusCode.value__ } catch { 0 }
  Rec "TC-G11-03" "G11" ($sc3 -eq 401 -or $sc3 -eq 403) "401/403" "status=$sc3" "Auth required on consumption endpoint"
}

$r4 = Call GET "$CONSUMPTION/NONEXISTENT_KEY_9999"
Rec "TC-G11-04" "G11" ($r4.status -eq 404 -or ($r4.status -eq 200 -and $r4.body.data.Count -eq 0)) "404 or empty list" "status=$($r4.status) count=$($r4.body.data.Count)" "Non-existent lookup key behavior"

$first5 = $r.body.data | Select-Object -First 1
$ok5 = ($null -ne $first5) -and ($first5.PSObject.Properties.Name -contains "code") -and ($first5.PSObject.Properties.Name -contains "label")
Rec "TC-G11-05" "G11" $ok5 "fields: code, label present" "code=$($first5.code) label=$($first5.label) labelEn=$($first5.labelEn)" "Response shape: code, label, labelEn"

$rCons = Call GET "$CONSUMPTION/${UID}_VALID"
$inactInCons = $false
if ($rCons.body.data) {
  $found6 = $rCons.body.data | Where-Object { $_.code -eq "CODE_INACT" }
  $inactInCons = ($found6 -ne $null -and $found6.Count -gt 0)
}
Rec "TC-G11-06" "G11" (-not $inactInCons) "inactive detail not in consumption" "hasInact=$inactInCons" "Inactive detail excluded from consumption"

$r7a = Call GET "$CONSUMPTION/CURRENCY"
$r7b = Call GET "$CONSUMPTION/CURRENCY"
Rec "TC-G11-07" "G11" ($r7a.status -eq 200 -and $r7b.status -eq 200 -and $r7a.body.data.Count -eq $r7b.body.data.Count) "both 200 same count" "first=$($r7a.body.data.Count) second=$($r7b.body.data.Count)" "Cache: repeated call consistent"

$r8 = Call GET "$CONSUMPTION/GL_ACCOUNT_TYPE"
Rec "TC-G11-08" "G11" ($r8.status -eq 200 -and $r8.body.data.Count -gt 0) "200 GL_ACCOUNT_TYPE values" "status=$($r8.status) count=$($r8.body.data.Count)" "Seeded GL_ACCOUNT_TYPE consumption"

# ------------------------------------------------------------------
# SUMMARY
# ------------------------------------------------------------------
Write-Host ""
Write-Host "=========================================="
Write-Host "  RESULTS SUMMARY"
Write-Host "=========================================="
$total  = $results.Count
$passed = ($results | Where-Object { $_.status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.status -eq "FAIL" }).Count
Write-Host "TOTAL : $total"
Write-Host "PASSED: $passed"
Write-Host "FAILED: $failed"
Write-Host ""
Write-Host "--- FAILURES ---"
$results | Where-Object { $_.status -eq "FAIL" } | ForEach-Object {
  Write-Host "  [FAIL] $($_.tcId) [$($_.group)] - $($_.notes)"
  Write-Host "         Expected: $($_.expected)"
  Write-Host "         Actual  : $($_.actual)"
}
Write-Host ""
Write-Host "--- ALL RESULTS ---"
$results | Format-Table tcId, group, status, notes -AutoSize -Wrap

$results | ConvertTo-Json -Depth 4 | Out-File "tests\masterdata-test-results.json" -Encoding UTF8
Write-Host "Saved: tests\masterdata-test-results.json"
