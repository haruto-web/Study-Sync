param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = 'Stop'

function Test-Jbr($path) {
    if ([string]::IsNullOrWhiteSpace($path)) { return $false }
    return (Test-Path (Join-Path $path 'bin\java.exe'))
}

function Find-EmbeddedJbr {
    $localAppData = [Environment]::GetFolderPath('LocalApplicationData')
    $userProfile = [Environment]::GetFolderPath('UserProfile')

    # 1) Explicit env overrides
    if (Test-Jbr $env:ANDROID_STUDIO_JBR) { return $env:ANDROID_STUDIO_JBR }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_STUDIO_HOME)) {
        $jbrFromHome = Join-Path $env:ANDROID_STUDIO_HOME 'jbr'
        if (Test-Jbr $jbrFromHome) { return $jbrFromHome }
    }

    # 2) Common install locations
    $candidates = @(
        'C:\Program Files\Android\Android Studio\jbr',
        'C:\Program Files (x86)\Android\Android Studio\jbr',
        (Join-Path $localAppData 'Programs\Android Studio\jbr'),
        (Join-Path $userProfile 'AppData\Local\Programs\Android Studio\jbr')
    )

    foreach ($c in $candidates) {
        if (Test-Jbr $c) { return $c }
    }

    # 3) JetBrains Toolbox installs (best-effort)
    $toolboxRoot = Join-Path $localAppData 'JetBrains\Toolbox\apps\AndroidStudio'
    if (Test-Path $toolboxRoot) {
        $found = Get-ChildItem -Path $toolboxRoot -Directory -Recurse -Depth 4 -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -ieq 'jbr' } |
            Select-Object -First 1
        if ($found -and (Test-Jbr $found.FullName)) { return $found.FullName }
    }

    return $null
}

$jbr = Find-EmbeddedJbr
if (-not $jbr) {
    Write-Error @"
Could not find Android Studio embedded JBR.

Fix options:
- Install Android Studio (includes JBR), then re-run.
- Or set ANDROID_STUDIO_JBR to the JBR folder, e.g.
    `$env:ANDROID_STUDIO_JBR='C:\Program Files\Android\Android Studio\jbr'
"@
}

$env:JAVA_HOME = $jbr
$env:PATH = (Join-Path $jbr 'bin') + ';' + $env:PATH

Write-Host "Using embedded JBR: $jbr"

if (-not $GradleArgs -or $GradleArgs.Count -eq 0) {
    $GradleArgs = @('tasks')
}

& .\gradlew.bat @GradleArgs
exit $LASTEXITCODE
