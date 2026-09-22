# LearnTech Momentum

Mobile application for ITMDA3-34, Group 6.

It is a goal tracking app for a client who supplies learning management systems. Learners see the courses they are enrolled in, set their own goals, log progress, and earn points, badges and streaks. There is a monthly leaderboard. The admin side lets the client manage accounts, set up badges and points, and see who is engaged and who is falling behind.

Built as a native Android app in Java, with a Java backend and a MySQL database. The app reads course data from the client's platform through a read only API. All data we use is test data that we create ourselves.

## What is in this repository

| Folder | What it is |
|---|---|
| `app-android` | The Android application, written in Java |
| `backend` | The Java backend and database. Not built yet |

## Video tutorial for GitHub Desktop

[![Watch the video](https://img.youtube.com/vi/N6vPzWkPN_Y/hqdefault.jpg)](https://www.youtube.com/watch?v=N6vPzWkPN_Y)

## Setup, you only do this once

1. Accept the email invitation to this repository. If you do not have a GitHub account it will ask you to make one.
2. Download GitHub Desktop from desktop.github.com and install it.
3. Open GitHub Desktop and sign in with your GitHub account.
4. Go to File, then Clone repository. Pick Momentum from the list.
5. For the location, use something simple like `C:\Dev\Momentum`. Do not put it in OneDrive or anywhere that syncs, because it will fight with Android Studio and break things.
6. Open Android Studio, choose Open, and point it at the `app-android` folder inside where you cloned it.
7. Wait for Gradle to finish. The first time takes a few minutes.

If Android Studio asks about an SDK or says something is missing, let it download what it needs.

## Every time you work

Before you start:

1. Open GitHub Desktop
2. Click Fetch origin, and then Pull if it offers
3. Now open Android Studio and work

This pulls down whatever everyone else has done. If you skip it you will be working on old code and you will cause a conflict later.

When you finish:

1. Go back to GitHub Desktop
2. You will see a list of what you changed
3. Type a short summary of what you did, for example "Added login screen layout"
4. Click Commit to main
5. Click Push origin

Push at the end of every session, even if what you did is not finished. Sitting on changes for days is what causes the worst conflicts. A small push every day is much safer than one big push every week. And someone else could help finish or fix your code when they start working.

## Staying out of each other's way

We each take our own parts so that we are not editing the same files at the same time. The split will be decided on WhatsApp.

If you need to change something in someone else's part, message them first and wait for them to reply. It takes two minutes and it saves an hour of untangling later.

## If something goes wrong

If GitHub Desktop says there is a conflict, stop and message Eben. Do not guess and do not click things to make the message go away, because that is how work gets lost.

If Android Studio will not build after a pull, try File, then Sync Project with Gradle Files, before assuming something is broken.

## Things not to do

Do not upload files through the GitHub website. Use GitHub Desktop so that everything is tracked properly.

Do not commit anything with real data in it. Everything we use is test data.

Do not worry about the files that never show up in GitHub Desktop, like `local.properties` and the `build` folders. Those are meant to be ignored and they are different on each of our machines.
