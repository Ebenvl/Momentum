package com.example.momentum.data;

import android.app.Activity;
import android.app.AlertDialog;

import java.util.List;

// Easter eggs: tells the backend when the learner does something that might
// unlock a secret badge, and celebrates any that did.
public class EasterEggs {

    // The events the backend knows about
    public static final String LOGO_TAPPED = "logo_tapped";
    public static final String OPENED_TIMETABLE = "opened_timetable";
    public static final String OPENED_COACH = "opened_coach";
    public static final String ASKED_BARTHOLOMEW = "asked_bartholomew";

    // Reports an event. Quietly does nothing if it fails, it's only for fun.
    public static void report(Activity activity, String event) {
        if (!Session.isLoggedIn()) {
            return;
        }
        Api.get().reportEvent(event, new ApiCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> unlocked) {
                celebrate(activity, unlocked);
            }

            @Override
            public void onError(String message) {
                // Not worth bothering the learner about
            }
        });
    }

    // Shows a pop-up for each secret badge that was just found.
    public static void celebrate(Activity activity, List<String> unlocked) {
        if (activity.isFinishing() || unlocked == null) {
            return;
        }
        for (String name : unlocked) {
            new AlertDialog.Builder(activity)
                    .setTitle("Secret achievement unlocked!")
                    .setMessage("You found \"" + name + "\".\n\nSee it in Achievements, on your Profile.")
                    .setPositiveButton("Nice!", null)
                    .show();
        }
    }
}
