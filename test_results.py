import requests
import re

session = requests.Session()

# 1. Login as admin
resp = session.post("http://localhost:8080/login", data={"username":"admin", "password":"admin"})
print("Admin Login:", resp.status_code)

# 2. Get dashboard
resp = session.get("http://localhost:8080/admin/dashboard")
print("Admin Dashboard:", resp.status_code)

# Let's find an exam ID
match = re.search(r'/admin/exams/(\d+)/results', resp.text)
if match:
    exam_id = match.group(1)
    print("Found Exam ID:", exam_id)
    
    # 3. Get results
    resp = session.get(f"http://localhost:8080/admin/exams/{exam_id}/results")
    print("Admin Results Status:", resp.status_code)
    if "error" in resp.text.lower() or resp.status_code != 200:
        print("RESULTS PAGE ERROR!")
        print(resp.text[:1000])
    
    # Let's find an attempt ID
    attempt_match = re.search(r'/admin/attempts/(\d+)', resp.text)
    if attempt_match:
        attempt_id = attempt_match.group(1)
        print("Found Attempt ID:", attempt_id)
        
        # 4. Get attempt details
        resp = session.get(f"http://localhost:8080/admin/attempts/{attempt_id}")
        print("Admin Attempt Details Status:", resp.status_code)
        if "error" in resp.text.lower() or resp.status_code != 200:
            print("ATTEMPT DETAILS PAGE ERROR!")
            print(resp.text[:1000])
    else:
        print("No attempts found.")
else:
    print("No exams found on dashboard.")
