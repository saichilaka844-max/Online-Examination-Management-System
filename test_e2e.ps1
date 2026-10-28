$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Get-CsrfToken($url) {
    $response = Invoke-WebRequest -Uri $url -WebSession $session -UseBasicParsing
    if ($response.Content -match 'name="_csrf" value="([^"]+)"') {
        return $matches[1]
    }
    return ""
}

# 1. Login as Admin
Write-Host "1. Logging in as Admin..."
$csrfToken = Get-CsrfToken "$baseUrl/login"
$loginBody = @{
    username = "admin"
    password = "Admin@1234"
    _csrf = $csrfToken
}
$response = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body $loginBody -WebSession $session -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue

if ($response.StatusCode -eq 302) {
    Write-Host "Login successful."
} else {
    Write-Host "Login Failed. Status: $($response.StatusCode)"
    exit
}

# 2. Create Exam
Write-Host "2. Creating Exam..."
$csrfToken = Get-CsrfToken "$baseUrl/admin/exams/new"
$examBody = @{
    title = "E2E PowerShell Exam NEW"
    subjectId = "1"
    durationMinutes = "30"
    passingPercentage = "50"
    availabilityMode = "NO_EXPIRY"
    startDateTime = ""
    endDateTime = ""
    "questions[0].questionText" = "What is 2+2?"
    "questions[0].optionA" = "1"
    "questions[0].optionB" = "2"
    "questions[0].optionC" = "3"
    "questions[0].optionD" = "4"
    "questions[0].correctAnswer" = "D"
    "questions[0].positiveMarks" = "2"
    _csrf = $csrfToken
}

$response = Invoke-WebRequest -Uri "$baseUrl/admin/exams/new" -Method Post -Body $examBody -WebSession $session -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue

if ($response.StatusCode -eq 302) {
    Write-Host "Exam Created!"
} else {
    Write-Host "Create Exam Failed. Status: $($response.StatusCode)"
    exit
}

# 3. Find Exam ID
Write-Host "3. Finding Exam ID..."
$response = Invoke-WebRequest -Uri "$baseUrl/admin/exams" -WebSession $session -UseBasicParsing
$matches = [regex]::Matches($response.Content, 'href="/admin/exams/(\d+)/edit"')
if ($matches.Count -gt 0) {
    $examId = $matches[$matches.Count - 1].Groups[1].Value
    Write-Host "Found Exam ID: $examId"
} else {
    Write-Host "Could not find newly created exam in the list!"
    exit
}

# 4. Edit Exam (Add another question)
Write-Host "4. Editing Exam $examId..."
$csrfToken = Get-CsrfToken "$baseUrl/admin/exams/$examId/edit"

# Get existing question ID from the edit form
$response = Invoke-WebRequest -Uri "$baseUrl/admin/exams/$examId/edit" -WebSession $session -UseBasicParsing
$q1Id = ""
if ($response.Content -match 'name="questions\[0\].id" value="(\d+)"') {
    $q1Id = $matches[1]
    Write-Host "Found Question 1 ID: $q1Id"
}

$editBody = @{
    title = "E2E PowerShell Exam Edited"
    subjectId = "1"
    durationMinutes = "30"
    passingPercentage = "50"
    availabilityMode = "NO_EXPIRY"
    startDateTime = ""
    endDateTime = ""
    "questions[0].id" = $q1Id
    "questions[0].questionText" = "What is 2+2? (Edited)"
    "questions[0].optionA" = "1"
    "questions[0].optionB" = "2"
    "questions[0].optionC" = "3"
    "questions[0].optionD" = "4"
    "questions[0].correctAnswer" = "D"
    "questions[0].positiveMarks" = "2"
    
    "questions[1].questionText" = "What is 3+3?"
    "questions[1].optionA" = "4"
    "questions[1].optionB" = "5"
    "questions[1].optionC" = "6"
    "questions[1].optionD" = "7"
    "questions[1].correctAnswer" = "C"
    "questions[1].positiveMarks" = "2"
    _csrf = $csrfToken
}

$response = Invoke-WebRequest -Uri "$baseUrl/admin/exams/$examId/edit" -Method Post -Body $editBody -WebSession $session -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue

if ($response.StatusCode -eq 302 -and $response.Headers.Location -match '/admin/exams') {
    Write-Host "Exam Edited Successfully!"
} else {
    Write-Host "Edit Exam Failed. Status: $($response.StatusCode)"
    exit
}

# 5. Delete Exam
Write-Host "5. Deleting Exam $examId..."
$csrfToken = Get-CsrfToken "$baseUrl/admin/exams"
$deleteBody = @{
    _csrf = $csrfToken
}
$response = Invoke-WebRequest -Uri "$baseUrl/admin/exams/$examId/delete" -Method Post -Body $deleteBody -WebSession $session -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue

if ($response.StatusCode -eq 302 -and $response.Headers.Location -match '/admin/exams') {
    Write-Host "Exam Deleted Successfully!"
} else {
    Write-Host "Delete Exam Failed. Status: $($response.StatusCode)"
    exit
}

Write-Host "All tests passed!"
