$ErrorActionPreference = 'Stop'

# Enables the Windows virtualization features required by Docker Desktop's WSL2 engine.
$features = @(
    'Microsoft-Windows-Subsystem-Linux',
    'VirtualMachinePlatform'
)

$results = @()
foreach ($feature in $features) {
    # DISM exposes a reliable process exit code, unlike the previous cmdlet result object.
    & dism.exe /Online /Enable-Feature "/FeatureName:$feature" /All /NoRestart
    if ($LASTEXITCODE -notin @(0, 3010)) {
        throw "DISM failed to enable $feature with exit code $LASTEXITCODE"
    }

    $state = (Get-WindowsOptionalFeature -Online -FeatureName $feature).State
    if ($state -notin @('Enabled', 'EnablePending')) {
        throw "Windows feature $feature was not enabled. Current state: $state"
    }
    $results += "$feature=$state"
}

# Ensures the Windows hypervisor is allowed to launch during the next boot.
& bcdedit.exe /set hypervisorlaunchtype auto | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "bcdedit failed with exit code $LASTEXITCODE"
}

$resultPath = 'D:\JavaPrograms\baituan\outputs\docker-windows-features.txt'
$bootConfiguration = & bcdedit.exe /enum '{current}'
$results += 'hypervisorlaunchtype=auto'
$results += ($bootConfiguration | Select-String 'hypervisorlaunchtype' | ForEach-Object { $_.Line.Trim() })
$results += "configuredAt=$(Get-Date -Format o)"
[System.IO.File]::WriteAllLines($resultPath, $results, (New-Object System.Text.UTF8Encoding($false)))
