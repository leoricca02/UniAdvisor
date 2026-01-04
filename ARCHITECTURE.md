# System Architecture

## High-Level Architecture

UniAdvisor follows a client-server architecture composed of:

- Android Mobile Client
- RESTful Cloud API
- Cloud-based Authentication and Storage Services

---

## Android Application

### Architecture Pattern
- MVVM (Model-View-ViewModel)

### Core Components
- Jetpack Compose UI
- ViewModels for state management
- Repositories for data abstraction
- Retrofit for networking
- Room for local persistence

### Sensors & Hardware
- Accelerometer (Shake detection)
- Magnetometer + Accelerometer (Compass)
- Ambient Light Sensor
- CameraX for OCR scanning

---

## Backend API

### API Layer
- FastAPI asynchronous endpoints
- Modular routers per domain

### Data Layer
- SQLAlchemy ORM
- PostgreSQL relational database

### Security
- Firebase Authentication
- Token validation via Firebase Admin SDK

---

## Deployment

- Backend containerized using Docker
- Environment-based configuration
- Compatible with PaaS and IaaS platforms
