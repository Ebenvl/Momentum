
package com.example.momentum.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.momentum.MainActivity;
import com.example.momentum.R;
import com.example.momentum.data.OfflineStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "study_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        createNotificationChannel(context);

        Intent openAppIntent = new Intent(
                context,
                MainActivity.class
        );

        openAppIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                100,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        String[] message = reminderMessage(context);

        android.app.Notification notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(message[0])
                        .setContentText(message[1])
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message[1]))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

        NotificationManagerCompat.from(context)
                .notify(100, notification);
    }

    // What the reminder says, from the learner's saved data (see OfflineStore):
    // goals due today or tomorrow, and a streak that breaks if they skip today.
    // Without either, the general reminder. Gives back {title, text}.
    private String[] reminderMessage(Context context) {
        StringBuilder text = new StringBuilder();
        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        Calendar calendar = Calendar.getInstance();
        String today = day.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = day.format(calendar.getTime());

        try {
            // Active goals with a target date of today or tomorrow
            String goalsJson = OfflineStore.savedCopy(context, "/goals");
            if (goalsJson != null) {
                JSONArray goals = new JSONArray(goalsJson);
                for (int i = 0; i < goals.length(); i++) {
                    JSONObject goal = goals.getJSONObject(i);
                    if (!"active".equals(goal.optString("status")) || goal.isNull("targetDate")) {
                        continue;
                    }
                    String date = goal.getString("targetDate");
                    if (date.equals(today)) {
                        text.append("\"").append(goal.getString("title")).append("\" is due today. ");
                    } else if (date.equals(tomorrow)) {
                        text.append("\"").append(goal.getString("title")).append("\" is due tomorrow. ");
                    }
                }
            }

            // A streak that ends if today has no check-in or learning time yet
            String meJson = OfflineStore.savedCopy(context, "/me");
            String daysJson = OfflineStore.savedCopy(context, "/me/active-days");
            if (meJson != null && daysJson != null) {
                int streak = new JSONObject(meJson).optInt("currentStreak");
                boolean activeToday = daysJson.contains("\"" + today + "\"");
                if (streak > 0 && !activeToday) {
                    text.append("Your ").append(streak).append("-day streak ends today. ")
                            .append("A check-in or some learning keeps it going.");
                }
            }
        } catch (Exception e) {
            // Unreadable saved data: fall back to the general reminder
        }

        if (text.length() == 0) {
            return new String[]{"Time to learn!", "Continue working on your courses and goals."};
        }
        return new String[]{"Don't lose your momentum", text.toString().trim()};
    }

    private void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Study reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription(
                    "Daily reminders to continue learning."
            );

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
