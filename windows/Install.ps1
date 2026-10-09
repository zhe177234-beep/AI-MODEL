$ErrorActionPreference = 'Stop'
$destination = Join-Path $env:LOCALAPPDATA 'AI-MODEL-Merchant\App'
$shortcutPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'AI-MODEL Merchant.lnk'
$shell = New-Object -ComObject WScript.Shell
if (Test-Path -LiteralPath $shortcutPath) {
    $existing = $shell.CreateShortcut($shortcutPath)
    if ($existing.Description -ne 'AI-MODEL Merchant desktop launcher') {
        throw 'A desktop shortcut with this name already exists. Rename it before installing.'
    }
}
New-Item -ItemType Directory -Path $destination -Force | Out-Null
# Copy only launcher files. Browser data and unrelated files remain untouched.
foreach ($name in @('Launch.ps1','config.json','Start.cmd','Open-in-browser.cmd','Uninstall-shortcut.cmd','Uninstall.ps1','README.txt')) {
    $source = Join-Path $PSScriptRoot $name
    $target = Join-Path $destination $name
    if ([IO.Path]::GetFullPath($source) -ne [IO.Path]::GetFullPath($target)) {
        Copy-Item -LiteralPath $source -Destination $target -Force
    }
}
$link = $shell.CreateShortcut($shortcutPath)
$link.TargetPath = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$link.Arguments = '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "' + (Join-Path $destination 'Launch.ps1') + '"'
$link.WorkingDirectory = $destination
$link.Description = 'AI-MODEL Merchant desktop launcher'
$link.Save()
Write-Host 'Installed. Open AI-MODEL Merchant on your desktop. No administrator rights required.'
