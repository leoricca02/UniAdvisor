# Installation & Setup Guide

## Prerequisites

- Android Studio (Ladybug or newer recommended)
- Python 3.9 or higher
- PostgreSQL (local installation or cloud instance)
- Firebase project with Authentication and Cloud Storage enabled

---

## Backend Setup

### 1. Navigate to the backend directory
```bash
cd uniadvisor-backend
```
### 2. Create a virtual environment and install dependencies
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```
### 3. Configure environment variables
Create a .env file in the backend root directory with the following content:
```bash
DATABASE_URL=postgresql://user:password@localhost/dbname
FIREBASE_CREDENTIALS=path/to/firebase-adminsdk.json
```
### 4. Run the backend server
```bash
uvicorn main:app --reload
```

Once started, the API documentation will be available at:
```bash
http://127.0.0.1:8000/docs
```

## Frontend Setup
### 1. Open the project
Open the uniadvisor-app directory using Android Studio.

### 2. Configure Firebase
Add your google-services.json file (downloaded from the Firebase Console) to:
```bash
uniadvisor-app/app/
```

### 3. Configure Google Maps API Key
Create a local.properties file in the root uniadvisor-app directory (if it does not already exist) and add:
```bash
MAPS_API_KEY=your_api_key_here
```

### 4. Build and run the application
- Sync the Gradle project
- Build the project
- Run the app on an emulator or a physical Android device