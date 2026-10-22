# Hosting the backend

How the backend is put online, so the app works without a laptop running it. The database runs on **Aiven** (free MySQL 8) and the backend on **Render** (free web service with HTTPS). Neither needs a card.

No passwords go in any file. Render keeps them as secret environment settings, and the backend reads them from there (see `backend/src/main/java/com/momentum/backend/config/Config.java`).

## 1. The database on Aiven

1. Sign up at [aiven.io](https://aiven.io) (Google or GitHub sign-in works).
2. **Create service**, choose **MySQL**, the **Free** plan, and the region closest to South Africa that the free plan offers. Name it `momentum-db`.
3. When it says **Running**, open it. From **Connection information**, note the **Host**, **Port**, **User** (`avnadmin`) and **Password**.
4. In MySQL Workbench, add a connection with those details. On the **SSL** tab, set **Use SSL** to **Require**. Test it.
5. Make a long random password for the app's database user. In PowerShell:

   ```powershell
   $b = New-Object byte[] 24; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
   ```

6. On the Aiven connection, open `backend/database/hosted_setup.sql`, replace `CHANGE_ME` with that password, and run it. Don't save the file with the password in it.
7. Move the data across:
   - Stop the backend on the laptop.
   - On the **laptop** connection: **Server**, **Data Export**. Tick `momentum`, choose **Dump Structure and Data** and **Export to Self-Contained File**, and untick **Include Create Schema**. Start the export.
   - On the **Aiven** connection: **Server**, **Data Import**. Choose **Import from Self-Contained File**, pick the file, set **Default Target Schema** to `momentum`, and start the import.
   - For an empty database instead, run `02_schema.sql` and then `05_extras.sql` on the Aiven connection.

## 2. The backend on Render

1. Make a JWT secret, a different long random text, with the same PowerShell line (change `24` to `48`).
2. Sign up at [render.com](https://render.com) with GitHub.
3. **New**, **Web Service**, and pick the **Momentum** repository.
   - **Name:** `momentum-backend`
   - **Branch:** `main`
   - **Root Directory:** `backend`
   - **Language:** `Docker`
   - **Instance Type:** `Free`
4. Under **Environment Variables**, add:

   | Name | Value |
   |---|---|
   | `DB_URL` | `jdbc:mysql://HOST:PORT/momentum?sslMode=REQUIRED` with Aiven's host and port |
   | `DB_USER` | `momentum_app` |
   | `DB_PASSWORD` | the app user's password from step 1.5 |
   | `JWT_SECRET` | the JWT secret from step 2.1 |
   | `LEARNTECH_BASE_URL` | `https://eduvos.learntech.co.za/api/app/v1` |
   | `LEARNTECH_API_KEY` | the LearnTech API key |
   | `MAIL_HOST` | `smtp.gmail.com` |
   | `MAIL_PORT` | `587` |
   | `MAIL_USERNAME` | the Gmail address |
   | `MAIL_PASSWORD` | the Gmail app password |
   | `MAIL_FROM` | the Gmail address |
   | `BACKUP_ENABLED` | `false` |

   Render sets `PORT` itself.
5. Under **Advanced**, set **Health Check Path** to `/health`.
6. **Create Web Service**. The first build takes a few minutes. The log should end with `Momentum backend running` and the first sync.
7. Open `https://YOUR-SERVICE.onrender.com/health` in a browser. It should say `{"status":"ok"}`.

## 3. The app

1. In `app-android/app/src/main/java/com/example/momentum/data/RealMomentumApi.java`, set `BASE_URL` to `https://YOUR-SERVICE.onrender.com`, with no `/` at the end.
2. In Android Studio: **Build**, **Generate Signed App Bundle or APK**, **APK**. Create a new key store the first time, and keep the key store file and its passwords safe and **out of the repository**. Choose **release**.
3. Install the APK on the phone and log in.

## Good to know

- **Sleeping:** the free service sleeps after 15 minutes with no requests. The first request after that takes up to a minute while it wakes up, and the app waits for it. To keep it awake during marking, a free [UptimeRobot](https://uptimerobot.com) monitor can open `/health` every 5 minutes.
- **Logins:** a new `JWT_SECRET` means everyone logs in again once, and fingerprint login asks for the password once.
- **Saved LearnTech settings** from the admin screen were encrypted with the old secret, so the backend falls back to `LEARNTECH_BASE_URL` and `LEARNTECH_API_KEY` until they are saved again.
- **Backups:** Aiven backs up its own databases, so the backend's own backup is turned off on Render.
- **Admin account:** use a strong password, and turn on two-step login for it, before the server is shared with anyone.
