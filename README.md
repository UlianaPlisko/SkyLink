# 🌌 SkyLink

**SkyLink** is a native Android stargazing app with a Spring Boot backend. Point your phone at the night sky and the app shows the stars, constellations and planets that are actually above you, using your location and the phone's orientation sensors. It also has a social side: chat rooms, astronomy events and favourites.

> Course project for *Introduction to Mobile and Wireless Technologies*, Slovak University of Technology in Bratislava (FIIT STU).
> Authors: **Kalyna Marharyta** (136183), **Plisko Uliana** (128348)

---

## Table of contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [How the sky view works](#how-the-sky-view-works)
4. [Tech stack](#tech-stack)
5. [Repository structure](#repository-structure)
6. [Database](#database)
7. [Deployment guide](#deployment-guide)
8. [API overview](#api-overview)
9. [Testing](#testing)
10. [Known limitations](#known-limitations)

---

## Features

**Sky observation**
- Live sky map (custom `SkyView` canvas) aligned with where the phone is pointing
- Stars, constellations, planets and deep-sky objects with details and Wikipedia info
- Search for objects, plus different cultural constellation systems (sky cultures)
- Weather / visibility widget for observing conditions
- **Red Night Mode** that keeps your eyes dark-adapted

**Social**
- Real-time chat rooms over WebSocket
- Astronomy events with a calendar, enrollment and capacity limits
- Push reminders 24 h, 12 h, 30 min and 5 min before an event (tapping opens the Calendar screen via deep link)

**User features**
- Email/password registration and login (JWT), plus **Sign in with Google**
- Roles: `OBSERVER` and `CONTRIBUTOR` (only contributors can create events)
- Profile with picture, favourites with editable notes

**Mobile-specific**
- Offline mode: Room cache, with a queue of pending actions that syncs when you are back online
- GPS location, or manual coordinates if permission is denied
- Runtime permission handling (location, notifications)

---

## Architecture

```
┌────────────────────────┐   REST (JSON) + JWT    ┌───────────────────────────┐
│ Android app (Kotlin)   │ ─────────────────────► │ Spring Boot backend       │
│ Retrofit · Room · Coil │ ◄───────────────────── │ Kotlin · Spring Security  │
│ SensorManager · Fused  │   WebSocket /ws-chat   │ JPA/Hibernate · Scheduler │
│ Location · FCM         │ ◄────────────────────► │                           │
└──────────┬─────────────┘                        └─────┬──────────┬──────────┘
           │ push                                       │          │
           ▼                                            ▼          ▼
   Firebase Cloud Messaging ◄────────────────── Firebase Admin   PostgreSQL
                                                                 (remote)
                                                      │
                                                      ▼
                                               WeatherAPI.com, Wikipedia
```

---

## How the sky view works

1. **Location**: `FusedLocationProviderClient` gives latitude, longitude and altitude. If GPS is off or denied, the manual location from *Settings → Location* is used.
2. **Orientation**: `OrientationHelper` / `CompassController` read the device **rotation vector** sensor. This is a fused sensor combining gyroscope, accelerometer and magnetometer, which is more stable than the raw gyroscope. Fallbacks are the geomagnetic and game rotation vectors. The rotation matrix is remapped for the current screen rotation, then azimuth, pitch and roll are derived.
3. **True north**: `GeomagneticField` computes magnetic declination so the compass points to true north, not magnetic north.
4. **Astronomy maths**: `MainViewModel` computes local sidereal time and converts each object's RA/Dec (stored in the database) to Altitude/Azimuth for your position and the current time.
5. **Rendering**: `SkyView` projects the visible objects onto the screen according to where the phone points, updating continuously.

---

## Tech stack

| Layer | Technology |
|---|---|
| Android | Kotlin, AndroidX, Material, Retrofit + Gson + OkHttp, Room (KSP), Coil, Coroutines, Play Services Location, Credential Manager (Google Sign-In), Firebase Messaging |
| Android SDK | `minSdk 24`, `targetSdk 36`, Android Gradle Plugin 9.1 |
| Backend | Kotlin 2.2, Spring Boot 4.0.2, Spring Web/WebFlux, Spring Security + JWT (jjwt), Spring Data JPA, WebSocket, springdoc OpenAPI (Swagger UI), Firebase Admin SDK |
| Database | PostgreSQL |
| Build / test | Gradle (wrapper included), JUnit 5, Mockito-Kotlin, JaCoCo, Robolectric, Espresso |
| Java | Backend: JDK 17+. Android unit tests: JDK 21 toolchain |

---

## Repository structure

```
SkyLink/
├── backend/                         Spring Boot service
│   ├── build.gradle
│   └── src/
│       ├── main/kotlin/com/skylink/backend/
│       │   ├── config/              Security, JWT filter, WebSocket, Firebase, DataSource, scheduler
│       │   ├── controller/          Auth, Celestial, Favorite, Event, ChatRoom, User(Profile), Weather
│       │   ├── service/             Business logic
│       │   ├── repository/          Spring Data JPA repositories
│       │   ├── model/               JPA entities and enums
│       │   ├── dto/                 Request/response objects
│       │   └── client/              WeatherAPI client
│       ├── main/resources/application.properties
│       └── test/                    Unit and integration tests
└── frontend/                        Android app (package com.codepalace.accelerometer)
    ├── app/
    │   ├── google-services.json     Firebase client config
    │   └── src/main/java/.../
    │       ├── ui/                  Activities, ViewModels, custom views (SkyView, compass)
    │       ├── sensors/             OrientationHelper, CompassController
    │       ├── api/                 Retrofit interfaces, WebSocket manager, DTOs
    │       ├── data/                Room DB, DAOs, repositories, session/settings storage
    │       ├── auth/                Google sign-in
    │       ├── notification/        FCM service
    │       └── config/ApiConfig.kt  Backend URL
    └── build.gradle.kts
```

---

## Database

The backend uses **PostgreSQL**. In development and in the demo, the database ran **remotely** (hosted PostgreSQL), not on a local machine. It was filled **once from an external astronomical dataset** (stars, constellations, planets, deep-sky objects and constellation cultures). The backend itself only reads this data. It never imports or generates it at startup.

Important points:

- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **does not create tables**. It only checks that the existing schema matches the entities, and the backend will fail to start if the schema is missing or different.
- The repository **does not contain** the SQL dump or the dataset import script. To run your own instance you need either access to the existing database or a database with the same schema, populated with your own data.
- Tables (see `model/entity`): `Users`, `UserCredentials`, `UserPfp`, `SpaceObject`, `StarData`, `PlanetData`, `DeepSkyObjectData`, `ObjectName`, `Constellation`, `ConstStarConnection`, `ConstellationCulture`, `Favorite`, `Events`, `EventParticipant`, `ChatRoom`, `ChatMessage`, `UserChatSubscription`.
- Celestial objects are stored by right ascension and declination in degrees (`ra_deg`, `dec_deg`) with magnitude. The app converts these to the local sky on the device.

The user-related tables (users, favourites, events, chats) are filled by the app. The celestial tables come from the dataset.

---

## Deployment guide

### Prerequisites

- JDK 17 or newer (JDK 21 recommended, since the Android unit tests use a 21 toolchain)
- Android Studio (current stable) with Android SDK 36
- A PostgreSQL database that already contains the SkyLink schema and data
- A Firebase project (for push notifications)
- A Google Cloud OAuth **Web client ID** (for Google Sign-In)
- *(Optional)* a free [WeatherAPI.com](https://www.weatherapi.com/) key for the weather widget
- A phone or emulator. A **physical phone is strongly recommended**, because emulators have no real rotation-vector sensor.

### 1. Clone

```bash
git clone https://github.com/UlianaPlisko/SkyLink.git
cd SkyLink
```

### 2. Prepare the database

Create or obtain a PostgreSQL database with the schema and celestial data described in [Database](#database). Note the JDBC URL, username and password.

### 3. Set up Firebase

1. Create a project in the [Firebase console](https://console.firebase.google.com/) and enable **Cloud Messaging**.
2. **Android app**: register an Android app with package name `com.codepalace.accelerometer`, download `google-services.json` and place it at `frontend/app/google-services.json`.
3. **Backend**: *Project settings → Service accounts → Generate new private key*. Save the file as
   `backend/src/main/resources/serviceAccountKey.json`
   (the backend loads it from the classpath, see `FirebaseConfig`).


### 4. Set up Google Sign-In

1. In Google Cloud Console, create an OAuth client of type **Web application** and note the client ID.
2. Create an **Android** OAuth client with package `com.codepalace.accelerometer` and your debug/release SHA-1 fingerprint.
3. Put the **Web** client ID in `frontend/app/src/main/res/values/strings.xml` as `web_client_id`.
4. Give the same Web client ID to the backend as `GOOGLE_ID` (next step).

### 5. Run the backend

The backend reads its configuration from environment variables:

| Variable | Required | Description |
|---|---|---|
| `APP_DATASOURCE_URL` | yes | e.g. `jdbc:postgresql://HOST:5432/DBNAME` |
| `APP_DATASOURCE_USERNAME` | yes | DB user |
| `APP_DATASOURCE_PASSWORD` | yes | DB password |
| `JWT_SECRET` | yes | Long random string used to sign JWTs (32+ characters) |
| `GOOGLE_ID` | yes | Google OAuth Web client ID |
| `WEATHER_API_KEY` | no | WeatherAPI.com key (weather widget) |

Linux / macOS:

```bash
cd backend
export APP_DATASOURCE_URL="jdbc:postgresql://HOST:5432/DBNAME"
export APP_DATASOURCE_USERNAME="user"
export APP_DATASOURCE_PASSWORD="password"
export JWT_SECRET="replace-with-a-long-random-secret"
export GOOGLE_ID="xxxx.apps.googleusercontent.com"
export WEATHER_API_KEY="your-key"
./gradlew bootRun
```

Windows (PowerShell):

```powershell
cd backend
$env:APP_DATASOURCE_URL="jdbc:postgresql://HOST:5432/DBNAME"
$env:APP_DATASOURCE_USERNAME="user"
$env:APP_DATASOURCE_PASSWORD="password"
$env:JWT_SECRET="replace-with-a-long-random-secret"
$env:GOOGLE_ID="xxxx.apps.googleusercontent.com"
.\gradlew.bat bootRun
```

The server listens on `0.0.0.0:8080`. Check it at **http://localhost:8080/swagger-ui.html**.

To build a standalone jar for a server or VPS:

```bash
./gradlew bootJar
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar   # with the env vars above
```

Check the exact jar name in `build/libs/`. For production, put the backend behind HTTPS (for example nginx or Caddy with Let's Encrypt).

### 6. Point the app to your backend

Edit `frontend/app/src/main/java/com/codepalace/accelerometer/config/ApiConfig.kt`:

```kotlin
object ApiConfig {
    const val BASE_URL = "http://<YOUR_BACKEND_HOST>:8080"
}
```

| Where the app runs | Use |
|---|---|
| Android emulator, backend on your PC | `http://10.0.2.2:8080` |
| Real phone, backend on your PC (same Wi-Fi) | `http://<your-PC-LAN-IP>:8080` |
| Deployed server with HTTPS | `https://your-domain` (the WebSocket automatically becomes `wss://…/ws-chat`) |

Cleartext HTTP is currently allowed in `network_security_config.xml` for development. Remove this for production and use HTTPS.

### 7. Build and run the Android app

1. Open the `frontend/` folder in Android Studio and let Gradle sync.
2. Select a device (a real phone is best) and press **Run ▶**.
3. Or from the command line:
   ```bash
   cd frontend
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
4. On first launch, grant **location** and **notification** permissions. Without location access, enter coordinates manually in *Settings → Location*.

### Quick checklist

- [ ] Database reachable and schema present (`ddl-auto=validate`)
- [ ] `serviceAccountKey.json` in `backend/src/main/resources/`
- [ ] `google-services.json` in `frontend/app/`
- [ ] All environment variables set
- [ ] `ApiConfig.BASE_URL` points to the backend
- [ ] Phone and backend can reach each other

---

## API overview

Public endpoints: `/api/auth/**`, `/api/celestial/**`, `/api/weather/**`, Swagger and `/ws-chat`. Everything else requires `Authorization: Bearer <JWT>`. Full interactive docs are in Swagger UI.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/google`, `/google/complete` |
| Celestial | `GET /api/celestial/space-objects`, `/bright-stars`, `/space-objects/{id}`, `/space-objects/{id}/wiki`, `/constellations`, `/cultures`, … |
| Weather | `GET /api/weather/current-visibility` |
| Profile | `GET/PUT /api/profile`, `PUT /api/profile/password`, `POST/GET/DELETE /api/profile/pfp`, `POST /api/profile/fcm-token` |
| Favorites | `GET/POST /api/favorites`, `PATCH/DELETE /api/favorites/{spaceObjectId}` |
| Events | `POST/GET /api/events`, `GET /api/events/date`, `/my`, `POST/DELETE /api/events/{id}/enroll`, `GET /api/events/{id}/is-participant` |
| Chat (REST) | `/api/chat/rooms`, `/{roomId}`, `/{roomId}/subscriptions`, `/{roomId}/messages`, `/{roomId}/read-state` |
| Chat (live) | WebSocket `ws://HOST:8080/ws-chat`: authenticate with a JWT, join a room, send messages. They are stored and broadcast to the room |

---

## Testing

```bash
# Backend (JUnit 5 + Mockito, JaCoCo report in backend/build/reports/jacoco)
cd backend && ./gradlew test

# Android unit tests (Robolectric)
cd frontend && ./gradlew testDebugUnitTest

# Android UI tests (needs a device or emulator)
cd frontend && ./gradlew connectedDebugAndroidTest
```

Instrumented tests use a mock backend (`MockBackend.kt`), so no real server is needed.

---

## Known limitations

- Backend `/ws-chat` allowed origins are still placeholders (`localhost:3000`, `your-frontend.com`). This does not affect the native app.
- The sky data depends on the PostgreSQL database being populated. The repo does not include the dataset or schema scripts.
- Sending chat messages and creating events require an internet connection, because they are not queued offline.
- The backend URL is a compile-time constant in `ApiConfig.kt`. Change it and rebuild when the server address changes.

