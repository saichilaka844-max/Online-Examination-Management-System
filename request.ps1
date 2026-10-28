$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Uri 'http://localhost:8080/login' -Method Post -Body @{username='admin';password='admin'} -WebSession $session | Out-Null
try {
    $res = Invoke-WebRequest -Uri 'http://localhost:8080/admin/attempts/1' -WebSession $session
    Write-Host 'Success'
} catch {
    Write-Host "Error Code: $($_.Exception.Response.StatusCode.value__)"
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $content = $reader.ReadToEnd()
    Write-Host "Response Body: $content"
}
