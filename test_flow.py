import requests
import re
from bs4 import BeautifulSoup

session = requests.Session()

def get_csrf(html):
    soup = BeautifulSoup(html, 'html.parser')
    csrf_input = soup.find('input', {'name': '_csrf'})
    return csrf_input['value'] if csrf_input else None

print("Fetching login page...")
r = session.get('http://localhost:8080/login')
csrf_token = get_csrf(r.text)

print("Logging in as student...")
r = session.post('http://localhost:8080/login', data={
    'username': 'student',
    'password': 'student',
    '_csrf': csrf_token
})

print("Fetching student dashboard...")
r = session.get('http://localhost:8080/student/dashboard')
match = re.search(r'/student/exams/(\d+)/start', r.text)
if not match:
    print("No exam available to start.")
else:
    exam_id = match.group(1)
    print(f"Starting exam {exam_id}...")
    r = session.get(f'http://localhost:8080/student/exams/{exam_id}/start')
    
    # We are in take-exam.html. Get the attemptId and form action
    csrf_token = get_csrf(r.text)
    soup = BeautifulSoup(r.text, 'html.parser')
    attempt_input = soup.find('input', {'name': 'attemptId'})
    if not attempt_input:
        print("Could not find attemptId on the page!")
    else:
        attempt_id = attempt_input['value']
        print(f"Submitting exam attempt {attempt_id}...")
        
        # Submitting the exam
        submit_data = {
            'attemptId': attempt_id,
            '_csrf': csrf_token
        }
        
        # Add random answers if inputs exist
        radios = soup.find_all('input', {'type': 'radio'})
        for radio in radios:
            # just select 'A' for everything to populate it
            if radio['value'] == 'A':
                submit_data[radio['name']] = 'A'

        r = session.post('http://localhost:8080/student/exams/submit', data=submit_data)
        print("Submitted. Check results.")

# Now login as admin and check the results view
print("Logging out...")
r = session.get('http://localhost:8080/login')

print("Logging in as admin...")
csrf_token = get_csrf(r.text)
r = session.post('http://localhost:8080/login', data={
    'username': 'admin',
    'password': 'admin',
    '_csrf': csrf_token
})

print("Fetching admin dashboard...")
r = session.get('http://localhost:8080/admin/dashboard')
match = re.search(r'/admin/exams/(\d+)/results', r.text)
if not match:
    print("No exam results available to view.")
else:
    exam_id = match.group(1)
    print(f"Viewing results for exam {exam_id}...")
    r = session.get(f'http://localhost:8080/admin/exams/{exam_id}/results')
    if r.status_code != 200:
        print(f"Error fetching results: {r.status_code}")
        print(r.text[:500])
    
    match = re.search(r'/admin/attempts/(\d+)', r.text)
    if not match:
        print("No attempts found.")
    else:
        attempt_id = match.group(1)
        print(f"Viewing attempt details for attempt {attempt_id}...")
        r = session.get(f'http://localhost:8080/admin/attempts/{attempt_id}')
        print(f"Attempt details status: {r.status_code}")
        if r.status_code != 200:
            print("ERROR on attempt details!")
        else:
            print("Successfully loaded attempt details!")
