# LearnTech Momentum

A mobile companion app for learners on LearnTech's learning platform. Learners see the courses they are enrolled on, set personal goals, check in on them, and earn points, badges and streaks. A monthly leaderboard adds some friendly competition. Administrators manage accounts, courses, badges and the platform connection, and can see who is engaged and who is falling behind.

Built for ITMDA3-34 (Mobile Application and Web Services) by Group 6 at Eduvos: Eben van Wyk van Loggerenberg, Ropafadzo Pakachena, Neo Mokoena and Oratile Mooketsi.

All data in the system is synthetic test data from LearnTech's sandbox. No real learner or company data is used.

## How it fits together

```
Android app (Java)  -->  Momentum backend (Java, Javalin)  -->  MySQL
                                     |
                                     +-->  LearnTech API (read only)
```

- The **Android app** only ever talks to our backend. It never holds the LearnTech API key and never touches the database directly.
- The **backend** owns everything we add on top of LearnTech: accounts, goals, check-ins, points, badges, streaks and logs, in our own MySQL database.
- The **LearnTech API** is read only. The backend syncs courses and learner progress from it, at startup, every 30 minutes, and for a learner when they log in. Nothing is ever written back to LearnTech.
- **Online**, the backend runs on Render (a free web service with HTTPS) and the database on Aiven (free hosted MySQL, with SSL required). The app points there by default, so it works anywhere with internet. On a hosting service the backend reads its passwords and keys from secret environment settings instead of a file.

## Features

### Learners

- Register and log in, or log in with a **fingerprint or face** once it's turned on
- **Stay logged in** between launches, until they log out or the login runs out after 7 days
- Optional **two-step login**: after the password, a code emailed to them
- **Forgot password**: a code emailed to them lets them choose a new password
- See their enrolled courses with their state (not started, in progress, completed) and the points each one earned, plus the rest of the course catalogue, with **search and filters**
- Open a course on LearnTech inside the app
- Set goals of different types, edit them, log check-ins on them, and **search and filter** them
- Goals that the app can check, like finishing a course or keeping a streak, **complete themselves** from the learner's data
- **Offline mode**: without a connection, the app still opens and shows their courses, goals, badges, profile and timetable as they were last synced. Changes need a connection
- **Reminders** at a time they choose, naming goals due today or tomorrow and warning when their streak is about to break
- Earn points for completed courses, badges and goals, and earn badges
- Keep a daily streak: a day counts when they spend at least 35% of a course's expected time learning in it on LearnTech, or log a check-in on a goal
- Find secret achievements (easter eggs) by exploring the app, each with its own XP
- A monthly and an all-time leaderboard
- A profile with all-time points, this month's points, the streak, and a **chart of points over time**
- Settings: change name and password, two-step login, fingerprint login, reminders, dark mode, and text size
- Achievements, a certificate for completed courses, a study timetable, a momentum map and study coaches

### Administrators

Everything a learner can do, plus:

- A dashboard with live numbers and recent activity
- User management: create, edit, suspend, reactivate, delete, and link an account to a LearnTech learner
- Course management: override the points a course is worth, and set an expected duration where LearnTech has none
- Badge management: create badges with a picture, a rule (courses completed, streak days or total points) and the points they give
- The LearnTech connection: change the platform address and API key, and test the connection. The key is stored encrypted on the server and only ever shown masked
- Export any table as a CSV file
- Engagement analytics and the system activity log

## How points work

A completed course is worth its expected minutes in points, so a 60-minute course is worth twice a 30-minute one. It only counts if the learner spent at least 70% of the expected time in it, so rushed completions earn nothing. Time spent is only a check, never the score. An administrator can override the points of any course.

Fast Course (LearnTech course 58) is the demo course. It only takes about 10 seconds, and the API reports time in whole minutes, so it can never pass the time check. It gives a random 1 to 30 points instead, so points can be shown live.

Each course, badge and goal can only award its points once, so a repeated sync never counts anything twice.

## Security

- Passwords are hashed with BCrypt and never stored or sent back as plain text
- Logins return a signed token (JWT) that lasts 7 days. Every request checks it against the account, so a suspended or deleted account loses access straight away, and roles are always read fresh
- Repeated wrong logins are blocked for 5 minutes, per email and per address
- Optional two-step login by email. Codes are 6 digits, stored only as a hash, work once, run out after 10 minutes, and stop working after 5 wrong tries. Turning it off needs the password
- Forgot password gives the same answer whether the email has an account or not, so it can't be used to find out who is registered
- Every database query uses prepared statements
- Learners can only see and change their own goals, check-ins and study sessions. Administrator routes check the role on the server
- The LearnTech API key stays on the server, encrypted when saved from the app
- Fingerprint login saves only the login token, encrypted with a key in the phone's secure key storage that only unlocks with a fingerprint or face. The password is never saved
- Staying logged in also saves only the token, encrypted with a key in the phone's secure key storage. The offline copies are in the app's private storage, one set per account, and logging out wipes both
- The app only allows plain `http` to the computer it's tested on. The hosted backend uses HTTPS, and the hosted database only accepts SSL connections. The app's database user can read and change data, but can't change or drop tables
- On a laptop, the backend backs up the database once a day. Online, the database service keeps its own backups

## What's in this repository

| Folder | What it is |
|---|---|
| `app-android` | The Android app |
| `backend` | The backend, and the database scripts in `backend/database` |

**App** (`app-android/app/src/main/java/com/example/momentum`):

| Folder | What it is |
|---|---|
| `MainActivity.java` | The main screens: Home, Courses, Goals, Leaderboard and Profile |
| `activities` | The other learner screens: login, sign up, settings, achievements, LearnTech, certificate, reminders, timetable and coaches |
| `admin` | The administrator screens |
| `service` | What the screens use, like logging in or saving a goal |
| `data` | Talks to the backend, keeps the login session, the offline copies, settings and fingerprint login |
| `model` | Simple classes for users, courses, goals, badges and so on |

**Backend** (`backend/src/main/java/com/momentum/backend`):

| Folder | What it is |
|---|---|
| `App.java` | Starts the server, the routes, the scheduled sync and the daily backup |
| `auth` | Register, login, login tokens and the login attempt limit |
| `profile`, `courses`, `goals`, `leaderboard`, `sessions`, `admin` | The routes for each part of the app |
| `db` | The database queries, one class per table or area |
| `sync` | Copies courses and learner progress from LearnTech |
| `rewards` | Awards badges and completes goals |
| `learntech` | The LearnTech API client |
| `backup` | The daily database backup |
| `config` | Reads `config.properties` (or the hosting service's environment settings) and encrypts the saved API key |
| `model` | The classes sent to and from the app |

## Running it

### What you need

- Java 25 (the backend's build downloads it if needed)
- MySQL 8
- Android Studio, and an Android phone with USB debugging, or the emulator

### 1. Database

In MySQL Workbench, logged in as root:

1. Open `backend/database/01_setup.sql`, change `CHANGE_ME` to a password for the app's database user, and run it. Don't save the file with the real password in it.
2. Run `backend/database/02_schema.sql`. It creates every table.

3. Run `backend/database/05_extras.sql`. It adds the secret achievements and Bartholomew Crownelius the Third, and on an older database also the tables for the streak and the easter eggs.

`03_study_sessions.sql` and `04_two_factor.sql` are only for a database set up before those features existed. Each adds what's missing without deleting anything. A database made with the current `02_schema.sql` already has both.

### 2. Backend

1. In the `backend` folder, copy `config.example.properties` and name the copy `config.properties`.
2. Fill in the database password from step 1, a long random `jwt.secret`, and the LearnTech API key. `config.properties` is in `.gitignore`, so it never goes to GitHub.
3. Optional, for two-step login and forgot password: fill in the `mail.` settings. For Gmail, use an app password (Google Account, Security, 2-Step Verification, App passwords), not the account's normal password. Without them, the app says email isn't set up. Render's free plan blocks outgoing email, so on the hosted backend these emails don't arrive.
4. Start it from the `backend` folder:

   ```
   .\gradlew run
   ```

It's running when it prints `Momentum backend running on http://localhost:7070`, followed by the first sync. Open `http://localhost:7070/health` in a browser to check.

If it says "Communications link failure", MySQL isn't running. Start the MySQL80 service.

### 3. App

1. Open the `app-android` folder in Android Studio and let Gradle finish.
2. The app talks to the backend at the address in `data/RealMomentumApi.java` (`BASE_URL`). It's set to the hosted backend, so nothing needs changing to use that. The free server sleeps after 15 minutes without use, so the first request after that can take up to a minute.
   - **Phone over USB, backend on the same computer:** use `http://localhost:7070`, and run `adb reverse tcp:7070 tcp:7070` once after plugging in, so the phone's `localhost` reaches the computer.
   - **Emulator, backend on the same computer:** use `http://10.0.2.2:7070`.
3. Run the app.

### Accounts

Register in the app. An account is linked to a LearnTech learner automatically when its email matches theirs, and their courses and points come through on the next sync or login. To make an administrator, change the account's `role` to `administrator` in the `Users` table once. After that, administrators can manage roles from the app.

## Main backend routes

All routes except register, login, the password reset and `/health` need the login token. Admin routes, `/health/db` and `/health/learntech` also need the administrator role.

| Route | What it does |
|---|---|
| `POST /register`, `POST /login`, `POST /login/verify` | Create an account, log in, the emailed two-step code |
| `POST /password/forgot`, `POST /password/reset` | Email a reset code, reset the password with it |
| `GET /me`, `PUT /me`, `PUT /me/password` | Your profile, points and streak; change your name or password |
| `/me/two-factor/start`, `/confirm`, `/disable` | Turn two-step login on (with an emailed code) or off (with the password) |
| `GET /me/courses`, `GET /courses` | Your courses, the whole catalogue |
| `GET /me/badges`, `GET /me/progress` | Your badges, your progress over time |
| `GET /me/active-days`, `GET /me/secrets`, `POST /me/events` | Days that counted towards the streak, secret achievements found, easter egg events |
| `/goals`, `/goals/{id}`, `/goals/{id}/checkins` | Goals and check-ins |
| `GET /leaderboard`, `GET /leaderboard?period=all` | This month's rankings, all-time rankings |
| `/study-sessions` | The study timetable |
| `/admin/users`, `/admin/courses`, `/admin/badges` | Manage users, courses and badges |
| `/admin/settings`, `POST /admin/settings/test` | The LearnTech connection |
| `GET /admin/stats`, `/admin/course-stats`, `/admin/logs` | Dashboard numbers and the activity log |
| `GET /admin/export/{table}` | A table as CSV |

## Working on the project

- Use GitHub Desktop. Fetch and pull before you start, and commit and push when you finish, even if it isn't done.
- Never commit `config.properties`, backups, or anything with real data in it.
- If GitHub Desktop shows a conflict, stop and ask before clicking anything.
- If Android Studio won't build after a pull, try File, then Sync Project with Gradle Files.
