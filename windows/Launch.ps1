param([switch]$Browser)
$ErrorActionPreference = 'Stop'
try {
    $config = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'config.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $uri = [Uri]$config.serviceUrl
    if (-not $uri.IsAbsoluteUri -or $uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.AbsoluteUri.Contains('"')) {
        throw 'The service URL must be an absolute HTTPS URL without embedded credentials.'
    }
    $url = $uri.AbsoluteUri
    $candidates = @()
    foreach ($root in @(${env:ProgramFiles(x86)}, $env:ProgramFiles, $env:LOCALAPPDATA)) {
        if ($root) {
            $candidates += Join-Path $root 'Microsoft\Edge\Application\msedge.exe'
            $candidates += Join-Path $root 'Google\Chrome\Application\chrome.exe'
        }
    }
    $executable = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if ($Browser -or -not $executable) {
        Start-Process -FilePath $url
    } else {
        # A dedicated browser profile keeps this workspace separate from personal browsing.
        $profile = Join-Path $env:LOCALAPPDATA 'AI-MODEL-Merchant\BrowserProfile'
        $arguments = '--app="' + $url + '" --user-data-dir="' + $profile + '"'
        Start-Process -FilePath $executable -ArgumentList $arguments
    }
} catch {
    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, 'AI-MODEL Merchant - launch failed') | Out-Null
    exit 1
}
