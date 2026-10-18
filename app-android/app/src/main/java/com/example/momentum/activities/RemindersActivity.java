
package com.example.momentum.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.momentum.R;

import java.util.Calendar;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String PREFS_NAME = "StudyReminderPrefs";
    private static final String KEY_HOUR = "reminderHour";
    private static final String KEY_MINUTE = "reminderMinute";
    private static final String KEY_ENABLED = "reminderEnabled";

    private TextView tvReminderTime;
    private TextView tvReminderStatus;

    private int selectedHour = 18;
    private int selectedMinute = 0;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        tvReminderTime = findViewById(R.id.tvReminderTime);
        tvReminderStatus = findViewById(R.id.tvReminderStatus);

        Button btnChooseTime = findViewById(R.id.btnChooseTime);
        Button btnSetReminder = findViewById(R.id.btnSetReminder);
        Button btnCancelReminder = findViewById(R.id.btnCancelReminder);
        Button btnReminderBack = findViewById(R.id.btnReminderBack);

        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        selectedHour = preferences.getInt(KEY_HOUR, 18);
        selectedMinute = preferences.getInt(KEY_MINUTE, 0);

        updateTimeText();

        if (preferences.getBoolean(KEY_ENABLED, false)) {
            tvReminderStatus.setText("Daily reminder is scheduled.");
        } else {
            tvReminderStatus.setText("No reminder scheduled yet.");
        }

        btnChooseTime.setOnClickListener(v -> showTimePicker());

        btnSetReminder.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33
                    && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_CODE
                );

                return;
            }

            scheduleReminder();
        });

        btnCancelReminder.setOnClickListener(v -> cancelReminder());

        btnReminderBack.setOnClickListener(v -> finish());
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;

                    updateTimeText();

                    preferences.edit()
                            .putInt(KEY_HOUR, selectedHour)
                            .putInt(KEY_MINUTE, selectedMinute)
                            .apply();
                },
                selectedHour,
                selectedMinute,
                false
        );

        dialog.setTitle("Choose your study time");
        dialog.show();
    }

    private void updateTimeText() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);

        String formattedTime = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
        );

        java.text.SimpleDateFormat formatter =
                new java.text.SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                );

        tvReminderTime.setText(formatter.format(calendar.getTime()));
    }

    private void scheduleReminder() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Toast.makeText(
                    this,
                    "Please enable notifications for LearnTech Momentum in Settings.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Toast.makeText(
                    this,
                    "Reminder scheduling is unavailable.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, selectedHour);
        alarmTime.set(Calendar.MINUTE, selectedMinute);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        // If today's selected time has passed, schedule for tomorrow.
        if (!alarmTime.after(Calendar.getInstance())) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                200,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        // Inexact repeating alarms are suitable for daily reminders
        // and do not require exact-alarm access.
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                alarmTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        preferences.edit()
                .putInt(KEY_HOUR, selectedHour)
                .putInt(KEY_MINUTE, selectedMinute)
                .putBoolean(KEY_ENABLED, true)
                .apply();

        tvReminderStatus.setText("Daily reminder is scheduled.");
        Toast.makeText(
                this,
                "Daily study reminder scheduled!",
                Toast.LENGTH_LONG
        ).show();
    }

    private void cancelReminder() {
        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                200,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        pendingIntent.cancel();

        preferences.edit()
                .putBoolean(KEY_ENABLED, false)
                .apply();

        tvReminderStatus.setText("Daily reminder cancelled.");

        Toast.makeText(
                this,
                "Study reminder cancelled.",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleReminder();
            } else {
                Toast.makeText(
                        this,
                        "Notification permission is needed for reminders.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
