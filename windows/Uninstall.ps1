$ErrorActionPreference = 'Stop'
$shortcutPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'AI-MODEL Merchant.lnk'
if (Test-Path -LiteralPath $shortcutPath) {
    $shell = New-Object -ComObject WScript.Shell
    $link = $shell.CreateShortcut($shortcutPath)
    if ($link.Description -eq 'AI-MODEL Merchant desktop launcher') {
        Remove-Item -LiteralPath $shortcutPath
        Write-Host 'Desktop shortcut removed.'
    } else { Write-Host 'Unrelated shortcut left unchanged.' }
}
Write-Host 'Browser data is retained. To remove launcher files, delete %LOCALAPPDATA%\AI-MODEL-Merchant\App manually.'
