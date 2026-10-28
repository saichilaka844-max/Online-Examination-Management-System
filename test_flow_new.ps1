$ErrorActionPreference = 'Stop'

function Get-CsrfToken($content) {
    if ($content -match '<input type="hidden" name="_csrf" value="([^"]+)"') {
        return $matches[1]
    }
    return $null
}

Write-Host "Fetching login page..."
$loginPage = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/login" -SessionVariable session
$csrf = Get-CsrfToken $loginPage.Content

Write-Host "Logging in as student..."
$loginResponse = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/login" -Method Post -WebSession $session -Body @{
    "username" = "student"
    "password" = "student"
    "_csrf" = $csrf
}

Write-Host "Fetching student dashboard..."
$dashboard = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/student/dashboard" -WebSession $session
if ($dashboard.Content -match '/student/exams/(\d+)/start') {
    $examId = $matches[1]
    Write-Host "Starting exam $examId..."
    $takeExam = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/student/exams/$examId/start" -WebSession $session
    
    if ($takeExam.Content -match '<input type="hidden" id="attemptId" name="attemptId" value="([^"]+)"' -or $takeExam.Content -match '<input type="hidden" name="attemptId" value="([^"]+)"' -or $takeExam.Content -match 'name="attemptId" value="([^"]+)"') {
        $attemptId = $matches[1]
        Write-Host "Attempt ID: $attemptId"
        
        $csrf2 = Get-CsrfToken $takeExam.Content
        Write-Host "Submitting exam..."
        
        # We don't really need to supply answers to trigger the logic
        $submit = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/student/exams/submit" -Method Post -WebSession $session -Body @{
            "attemptId" = $attemptId
            "_csrf" = $csrf2
        }
        Write-Host "Submitted successfully."
    } else {
        Write-Host "Could not find attemptId on the page!"
        Write-Host $takeExam.Content.Substring(0, 500)
    }
} else {
    Write-Host "No exam found for student."
}

Write-Host "Logging out..."
$logoutPage = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/login" -SessionVariable adminSession
$adminCsrf = Get-CsrfToken $logoutPage.Content

Write-Host "Logging in as admin..."
$adminLogin = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/login" -Method Post -WebSession $adminSession -Body @{
    "username" = "admin"
    "password" = "Admin@1234"
    "_csrf" = $adminCsrf
}

Write-Host "Fetching admin dashboard..."
$adminDash = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/admin/dashboard" -WebSession $adminSession
Write-Host "Viewing results for exam $examId..."

try {
    $results = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/admin/exams/$examId/results" -WebSession $adminSession
    Write-Host "Viewing attempt details for $attemptId..."
    
    try {
        $details = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/admin/attempts/$attemptId" -WebSession $adminSession
        Write-Host "Attempt details loaded successfully!"
    } catch {
        Write-Host "ERROR loading attempt details!"
        Write-Host $_.Exception.Message
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            Write-Host $reader.ReadToEnd()
        }
    }
} catch {
    Write-Host "ERROR loading results!"
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host $reader.ReadToEnd()
    }
}

