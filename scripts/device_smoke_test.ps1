<#
.SYNOPSIS
  Physical-device functional smoke-test harness for the LABDA app
  (ai.aminrezaei.dataloggerapp) on a connected Android device.

.DESCRIPTION
  Orchestrates the Compose instrumentation test classes under app/src/androidTest
  (test matrix sections A-F) in the order required by shared on-device app state,
  then performs pure-ADB checks for section G (force-stop/restart persistence),
  which self-instrumentation cannot perform on its own process.

  Room DB inspection never runs SQL against the live on-device database. Instead
  it pulls the .db/-wal/-shm files via `adb exec-out run-as <pkg> cat ...` into a
  local snapshot folder and queries that local copy with the SDK's own sqlite3.exe
  (read-only SELECT COUNT(*) queries only). The production database on the device
  is never written to by this script.

.PARAMETER SkipBuild
  Skip `gradlew assembleDebug assembleDebugAndroidTest`; reuse already-built APKs.

.PARAMETER SkipInstall
  Skip reinstalling the APKs onto the device.

.PARAMETER OnlyClass
  Run a single androidTest class (simple name, e.g. "CollectionServiceAndroidTest")
  instead of the full A-G matrix. Skips the pm clear + onboarding step; use this
  when the app is already past onboarding on the device.

.OUTPUTS
  - Console PASS/FAIL/INCONCLUSIVE lines as each check completes.
  - smoke_test_results\report_<timestamp>.md with the full matrix and evidence.
  - smoke_test_results\logs\<class>_<timestamp>.txt with raw `am instrument` output.
  - smoke_test_results\db_snapshots_<timestamp>\ with local DB/prefs snapshots.
#>

param(
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [string]$OnlyClass
)

$ErrorActionPreference = "Stop"

# ---- Configuration ----
$Pkg = "ai.aminrezaei.dataloggerapp"
$TestPkg = "$Pkg.test"
$MainActivityComponent = "$Pkg/.MainActivity"
$ServiceShortName = "LocationService"
$DbFileName = "DataLoggerApp_Dev.db"
$PrefsFileName = "LABDALoggerPrefs.xml"
$InstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Gradlew = Join-Path $RepoRoot "gradlew.bat"

$SdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
           elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
           else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$Adb = Join-Path $SdkRoot "platform-tools\adb.exe"
$LocalSqlite3 = Join-Path $SdkRoot "platform-tools\sqlite3.exe"

if (-not (Test-Path $Adb)) {
    throw "adb.exe not found at '$Adb'. Set ANDROID_HOME or ANDROID_SDK_ROOT."
}

$RunStamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ReportDir = Join-Path $RepoRoot "smoke_test_results"
$LogDir = Join-Path $ReportDir "logs"
$SnapshotDir = Join-Path $ReportDir "db_snapshots_$RunStamp"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
New-Item -ItemType Directory -Force -Path $SnapshotDir | Out-Null
$ReportPath = Join-Path $ReportDir "report_$RunStamp.md"

$Results = [System.Collections.Generic.List[object]]::new()

function Add-Result {
    param(
        [Parameter(Mandatory)][string]$Section,
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][ValidateSet("PASS", "FAIL", "INCONCLUSIVE")][string]$Status,
        [Parameter(Mandatory)][string]$Detail
    )
    $Results.Add([pscustomobject]@{ Section = $Section; Name = $Name; Status = $Status; Detail = $Detail })
    $color = switch ($Status) { "PASS" { "Green" }; "FAIL" { "Red" }; default { "Yellow" } }
    Write-Host "[$Status] ($Section) $Name - $Detail" -ForegroundColor $color
}

function Invoke-AdbShell {
    param([Parameter(Mandatory)][string]$Command)
    & $Adb shell $Command 2>&1
}

function Wait-ForDevice {
    Write-Host "Waiting for a connected ADB device..."
    & $Adb wait-for-device
    $deviceLines = & $Adb devices
    $connected = $deviceLines | Select-String -Pattern "\tdevice$"
    if (-not $connected) {
        throw "No ADB device in 'device' state. Check USB connection and debugging authorization.`n$($deviceLines -join "`n")"
    }
    Write-Host "Device ready: $($connected -join ', ')"
}

function Build-Apks {
    if ($SkipBuild) { Write-Host "Skipping build (-SkipBuild)."; return }
    Write-Host "Building debug + androidTest APKs..."
    & $Gradlew --no-daemon -p $RepoRoot assembleDebug assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
}

function Install-Apks {
    if ($SkipInstall) { Write-Host "Skipping install (-SkipInstall)."; return }
    $apk = Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $RepoRoot "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if (-not (Test-Path $apk)) { throw "Debug APK not found at '$apk' - run without -SkipBuild first." }
    if (-not (Test-Path $testApk)) { throw "androidTest APK not found at '$testApk' - run without -SkipBuild first." }

    Write-Host "Installing $apk ..."
    & $Adb install -r -g $apk
    if ($LASTEXITCODE -ne 0) { throw "adb install failed for app-debug.apk" }

    Write-Host "Installing $testApk ..."
    & $Adb install -r -g $testApk
    if ($LASTEXITCODE -ne 0) { throw "adb install failed for app-debug-androidTest.apk" }
}

function Reset-AppState {
    Write-Host "Resetting app state (pm clear) for a genuine first launch..."
    Invoke-AdbShell "pm clear $Pkg" | Out-Null
}

function Grant-BaselinePermissions {
    Write-Host "Granting baseline runtime permissions via ADB..."
    $perms = @(
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACTIVITY_RECOGNITION"
    )
    foreach ($p in $perms) {
        Invoke-AdbShell "pm grant $Pkg $p" | Out-Null
    }
}

function Get-ServiceRunningState {
    $dump = (Invoke-AdbShell "dumpsys activity services $Pkg") -join "`n"
    return $dump -match $ServiceShortName
}

function Invoke-InstrumentationClass {
    param(
        [Parameter(Mandatory)][string]$ClassName,
        [string]$MethodName
    )
    $target = if ($MethodName) { "$Pkg.$ClassName#$MethodName" } else { "$Pkg.$ClassName" }
    $label = if ($MethodName) { "${ClassName}_${MethodName}" } else { $ClassName }
    Write-Host "Running instrumentation: $target"
    $output = & $Adb shell am instrument -w -e class "$target" "$TestPkg/$InstrumentationRunner" 2>&1
    $text = $output -join "`n"

    $logPath = Join-Path $LogDir "${label}_$RunStamp.txt"
    Set-Content -Path $logPath -Value $text -Encoding utf8

    $passed = ($text -match "OK \(\d+ tests?\)") -and
              ($text -notmatch "FAILURES!!!") -and
              ($text -notmatch "INSTRUMENTATION_RESULT: shortMsg=") -and
              ($text -notmatch "Process crashed")

    return [pscustomobject]@{ Success = $passed; Output = $text; LogPath = $logPath }
}

function Add-InstrumentationResult {
    param([Parameter(Mandatory)][string]$Section, [Parameter(Mandatory)][string]$ClassName)
    $r = Invoke-InstrumentationClass -ClassName $ClassName
    $status = if ($r.Success) { "PASS" } else { "FAIL" }
    Add-Result -Section $Section -Name $ClassName -Status $status -Detail "see $($r.LogPath)"
    return $r
}

function Add-InstrumentationMethodResult {
    param(
        [Parameter(Mandatory)][string]$Section,
        [Parameter(Mandatory)][string]$ClassName,
        [Parameter(Mandatory)][string]$MethodName
    )
    # Onboarding test methods mutate persistent first-launch state (SharedPreferences),
    # so each one gets its own pm clear + permission grant rather than sharing on-device
    # state with whatever other method last ran in the same class - do not rely on
    # method ordering within a single `am instrument -e class` invocation.
    Reset-AppState
    Grant-BaselinePermissions
    $r = Invoke-InstrumentationClass -ClassName $ClassName -MethodName $MethodName
    $status = if ($r.Success) { "PASS" } else { "FAIL" }
    Add-Result -Section $Section -Name "$ClassName#$MethodName" -Status $status -Detail "see $($r.LogPath)"
    return $r
}

function New-DbSnapshot {
    param([Parameter(Mandatory)][string]$Label)
    $dir = Join-Path $SnapshotDir $Label
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    foreach ($suffix in @("", "-wal", "-shm")) {
        $remote = "databases/$DbFileName$suffix"
        $local = Join-Path $dir "$DbFileName$suffix"
        & $Adb exec-out run-as $Pkg cat $remote > $local 2>$null
        $item = Get-Item $local -ErrorAction SilentlyContinue
        if ($null -eq $item -or $item.Length -eq 0) {
            Remove-Item $local -ErrorAction SilentlyContinue
        }
    }
    return (Join-Path $dir $DbFileName)
}

function Get-TableCount {
    param([Parameter(Mandatory)][string]$DbPath, [Parameter(Mandatory)][string]$Table)
    if (-not (Test-Path $LocalSqlite3)) { return $null }
    if (-not (Test-Path $DbPath)) { return $null }
    $result = & $LocalSqlite3 $DbPath "SELECT COUNT(*) FROM $Table;" 2>&1
    if ($LASTEXITCODE -ne 0) { return $null }
    $lastLine = ($result | Select-Object -Last 1)
    $parsed = 0
    if ([int]::TryParse("$lastLine", [ref]$parsed)) { return $parsed }
    return $null
}

function Get-PrefsSnapshot {
    param([Parameter(Mandatory)][string]$Label)
    $dir = Join-Path $SnapshotDir $Label
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $local = Join-Path $dir $PrefsFileName
    & $Adb exec-out run-as $Pkg cat "shared_prefs/$PrefsFileName" > $local 2>$null
    $item = Get-Item $local -ErrorAction SilentlyContinue
    if ($null -eq $item -or $item.Length -eq 0) { return $null }
    return $local
}

function Get-PrefsBooleanValue {
    param([string]$PrefsPath, [string]$Key)
    if ([string]::IsNullOrEmpty($PrefsPath) -or -not (Test-Path $PrefsPath)) { return $null }
    $content = Get-Content $PrefsPath -Raw
    if ($content -match "<boolean name=`"$Key`" value=`"(true|false)`"\s*/>") {
        return [bool]::Parse($matches[1])
    }
    return $null
}

# ---- Main ----
Wait-ForDevice
Build-Apks
Install-Apks

if ($OnlyClass) {
    Add-InstrumentationResult -Section "Adhoc" -ClassName $OnlyClass | Out-Null
}
else {
    # Section A: onboarding requires a genuine first launch. Each method mutates
    # persistent first-launch state, so each runs as its own pm-clear'd smoke-test
    # case instead of sharing state with the other - do not rely on method ordering.
    Add-InstrumentationMethodResult -Section "A" -ClassName "OnboardingFlowTest" -MethodName "firstLaunch_learnMoreThenBack_returnsToWelcome" | Out-Null
    Add-InstrumentationMethodResult -Section "A" -ClassName "OnboardingFlowTest" -MethodName "firstLaunch_getStarted_autoAdvancesThroughPermissionsToDashboardOnce" | Out-Null

    # Sections B-F: run after onboarding, sharing the same on-device app state.
    Add-InstrumentationResult -Section "B" -ClassName "BottomNavigationTest" | Out-Null
    Add-InstrumentationResult -Section "C" -ClassName "CollectionServiceAndroidTest" | Out-Null
    Add-InstrumentationResult -Section "D" -ClassName "SettingsBehaviorAndroidTest" | Out-Null
    Add-InstrumentationResult -Section "E" -ClassName "DataScreenAndroidTest" | Out-Null
    Add-InstrumentationResult -Section "F" -ClassName "EmaWorkflowAndroidTest" | Out-Null

    # Section G: force-stop/restart persistence - pure ADB, no self-instrumentation
    # (a killed process cannot observe its own restart).
    Write-Host "`n--- Section G: force-stop/restart persistence ---"

    $beforeDb = New-DbSnapshot -Label "G_before"
    $accelBefore = Get-TableCount -DbPath $beforeDb -Table "accelerometer"
    $locationBefore = Get-TableCount -DbPath $beforeDb -Table "location"
    $deviceStateBefore = Get-TableCount -DbPath $beforeDb -Table "device_state"
    $emaBefore = Get-TableCount -DbPath $beforeDb -Table "ema_responses"
    $prefsBefore = Get-PrefsSnapshot -Label "G_before"
    $loggingLocationBefore = Get-PrefsBooleanValue -PrefsPath $prefsBefore -Key "logging_location"

    Invoke-AdbShell "am force-stop $Pkg" | Out-Null
    Start-Sleep -Seconds 2
    $runningAfterForceStop = Get-ServiceRunningState
    if (-not $runningAfterForceStop) {
        Add-Result -Section "G" -Name "Force-stop ends the foreground service" -Status "PASS" -Detail "LocationService absent from dumpsys after am force-stop"
    }
    else {
        Add-Result -Section "G" -Name "Force-stop ends the foreground service" -Status "FAIL" -Detail "LocationService still listed in dumpsys after am force-stop"
    }

    Invoke-AdbShell "am start -n $MainActivityComponent" | Out-Null
    Start-Sleep -Seconds 4

    $afterDb = New-DbSnapshot -Label "G_after"
    $accelAfter = Get-TableCount -DbPath $afterDb -Table "accelerometer"
    $locationAfter = Get-TableCount -DbPath $afterDb -Table "location"
    $deviceStateAfter = Get-TableCount -DbPath $afterDb -Table "device_state"
    $emaAfter = Get-TableCount -DbPath $afterDb -Table "ema_responses"
    $prefsAfter = Get-PrefsSnapshot -Label "G_after"
    $loggingLocationAfter = Get-PrefsBooleanValue -PrefsPath $prefsAfter -Key "logging_location"

    $countsDetail = "accelerometer $accelBefore->$accelAfter, location $locationBefore->$locationAfter, device_state $deviceStateBefore->$deviceStateAfter, ema_responses $emaBefore->$emaAfter"
    if ($null -eq $accelBefore -or $null -eq $accelAfter -or $null -eq $locationBefore -or $null -eq $locationAfter -or $null -eq $deviceStateBefore -or $null -eq $deviceStateAfter -or $null -eq $emaBefore -or $null -eq $emaAfter) {
        Add-Result -Section "G" -Name "Room row counts survive force-stop/restart" -Status "INCONCLUSIVE" -Detail "Could not read a DB snapshot (run-as or local sqlite3 unavailable) - see $SnapshotDir"
    }
    elseif ($accelAfter -ge $accelBefore -and $locationAfter -ge $locationBefore -and $deviceStateAfter -ge $deviceStateBefore -and $emaAfter -ge $emaBefore) {
        Add-Result -Section "G" -Name "Room row counts survive force-stop/restart" -Status "PASS" -Detail $countsDetail
    }
    else {
        Add-Result -Section "G" -Name "Room row counts survive force-stop/restart" -Status "FAIL" -Detail "A table's row count DECREASED across force-stop/restart: $countsDetail"
    }

    if ($null -eq $loggingLocationBefore -or $null -eq $loggingLocationAfter) {
        Add-Result -Section "G" -Name "Settings preferences survive force-stop/restart" -Status "INCONCLUSIVE" -Detail "Could not read shared_prefs XML via run-as - see $SnapshotDir"
    }
    elseif ($loggingLocationBefore -eq $loggingLocationAfter) {
        Add-Result -Section "G" -Name "Settings preferences survive force-stop/restart" -Status "PASS" -Detail "logging_location=$loggingLocationBefore unchanged across force-stop/restart"
    }
    else {
        Add-Result -Section "G" -Name "Settings preferences survive force-stop/restart" -Status "FAIL" -Detail "logging_location changed: $loggingLocationBefore -> $loggingLocationAfter"
    }
}

# ---- Report ----
$md = [System.Text.StringBuilder]::new()
[void]$md.AppendLine("# LABDA physical-device smoke test report")
[void]$md.AppendLine("")
[void]$md.AppendLine("Run: $RunStamp")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Section | Check | Status | Detail |")
[void]$md.AppendLine("|---|---|---|---|")
foreach ($r in $Results) {
    $detail = ($r.Detail -replace "\|", "\|") -replace "`n", " "
    [void]$md.AppendLine("| $($r.Section) | $($r.Name) | $($r.Status) | $detail |")
}
Set-Content -Path $ReportPath -Value $md.ToString() -Encoding utf8

Write-Host "`nReport written to $ReportPath"

$failCount = ($Results | Where-Object { $_.Status -eq "FAIL" }).Count
if ($failCount -gt 0) { exit 1 } else { exit 0 }
