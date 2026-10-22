
package com.example.momentum.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.Badge;
import com.example.momentum.model.Enrolment;
import com.example.momentum.model.Goal;
import com.example.momentum.model.User;
import com.example.momentum.service.CourseService;
import com.example.momentum.service.GoalService;
import com.example.momentum.service.ProfileService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementsActivity extends BaseActivity {

    private static final String TAG = "AchievementsActivity";

    private final CourseService courseService = new CourseService();
    private final GoalService goalService = new GoalService();
    private final ProfileService profileService = new ProfileService();
    private int userId;

    private TextView tvCompletedCourses;
    private TextView tvLearningProgress;
    private TextView tvCompletedGoals;

    private TextView tvFirstCourse;
    private TextView tvCourseMaster;
    private TextView tvFirstGoal;
    private TextView tvGoalCrusher;
    private TextView tvLearningStar;
    private TextView tvStudyStreak;

    private ProgressBar progressAchievements;

    private int completedCourses = 0;
    private int completedGoals = 0;
    private int overallProgress = 0;

    private SharedPreferences streakPreferences;
    private int studyStreak = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        tvCompletedCourses = findViewById(R.id.tvCompletedCourses);
        tvLearningProgress = findViewById(R.id.tvLearningProgress);
        tvCompletedGoals = findViewById(R.id.tvCompletedGoals);

        tvFirstCourse = findViewById(R.id.tvFirstCourse);
        tvCourseMaster = findViewById(R.id.tvCourseMaster);
        tvFirstGoal = findViewById(R.id.tvFirstGoal);
        tvGoalCrusher = findViewById(R.id.tvGoalCrusher);
        tvLearningStar = findViewById(R.id.tvLearningStar);

        progressAchievements = findViewById(R.id.progressAchievements);

        tvStudyStreak = findViewById(R.id.tvStudyStreak);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        if (!Session.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = Session.getUser().id;

        streakPreferences = getSharedPreferences(
                "StudyStreakPrefs_" + userId,
                MODE_PRIVATE
        );

        loadStudyStreak();
        loadAchievements();


        // The streak and badges the backend keeps, from check-ins and badge rules.
        loadBackendStreak();
        loadEarnedBadges();
    }


    // Shows the streak from the learner's check-ins, which the backend keeps.
    private void loadBackendStreak() {
        profileService.getMe(new ApiCallback<User>() {
            @Override
            public void onSuccess(User me) {
                int streak = me.currentStreak != null ? me.currentStreak : 0;
                tvStudyStreak.setText("🔥 Study streak: " + streak + (streak == 1 ? " day" : " days"));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Could not load the streak: " + error);
            }
        });
    }

    // Lists the badges the learner has earned, each with its points.
    private void loadEarnedBadges() {
        LinearLayout list = findViewById(R.id.llEarnedBadges);
        TextView none = findViewById(R.id.tvNoEarnedBadges);

        profileService.getMyBadges(new ApiCallback<List<Badge>>() {
            @Override
            public void onSuccess(List<Badge> badges) {
                list.removeAllViews();
                none.setVisibility(badges.isEmpty() ? View.VISIBLE : View.GONE);

                // One card per badge, in the same style as the cards above
                for (Badge badge : badges) {
                    TextView card = new TextView(AchievementsActivity.this);
                    String text = "🏅 " + badge.name + "  +" + badge.pointsAwarded + " XP";
                    if (badge.description != null && !badge.description.isEmpty()) {
                        text += "\n" + badge.description;
                    }
                    card.setText(text);
                    card.setTextColor(getResources().getColor(R.color.ltm_navy));
                    card.setTextSize(15);
                    card.setBackgroundResource(R.drawable.bg_card);
                    int padding = Math.round(16 * getResources().getDisplayMetrics().density);
                    card.setPadding(padding, padding, padding, padding);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
                    list.addView(card, lp);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Could not load badges: " + error);
            }
        });
    }

    private void loadAchievements() {
        loadCourses();
        loadGoals();
    }

    private void loadCourses() {
        courseService.getMyCourses(new ApiCallback<List<Enrolment>>() {
            @Override
            public void onSuccess(List<Enrolment> courses) {
                completedCourses = 0;
                int courseCount = courses.size();

                for (Enrolment course : courses) {
                    if ("completed".equals(course.state)) {
                        completedCourses++;
                    }
                }

                // Courses completed out of courses enrolled, the only
                // percentage LearnTech has
                overallProgress = courseCount > 0
                        ? Math.round(completedCourses * 100f / courseCount)
                        : 0;

                tvCompletedCourses.setText(
                        "Completed courses: " + completedCourses
                );

                tvLearningProgress.setText(
                        "Overall learning progress: " + overallProgress + "%"
                );

                updateBadges();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Could not load courses: " + error);
                Toast.makeText(
                        AchievementsActivity.this,
                        "Could not load course achievements",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void loadGoals() {
        goalService.getGoals(new ApiCallback<List<Goal>>() {
            @Override
            public void onSuccess(List<Goal> goals) {
                completedGoals = 0;

                for (Goal goal : goals) {
                    String status = goal.status;

                    if (status != null
                            && status.equalsIgnoreCase("completed")) {
                        completedGoals++;
                    }
                }

                tvCompletedGoals.setText(
                        "Completed goals: " + completedGoals
                );

                updateBadges();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Could not load goals: " + error);
                Toast.makeText(
                        AchievementsActivity.this,
                        "Could not load goal achievements",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void updateBadges() {
        setBadge(
                tvFirstCourse,
                completedCourses >= 1,
                "First Steps - Complete your first course"
        );

        setBadge(
                tvCourseMaster,
                completedCourses >= 5,
                "Course Master - Complete 5 courses"
        );

        setBadge(
                tvFirstGoal,
                completedGoals >= 1,
                "Goal Getter - Complete your first goal"
        );

        setBadge(
                tvGoalCrusher,
                completedGoals >= 5,
                "Goal Crusher - Complete 5 goals"
        );

        setBadge(
                tvLearningStar,
                overallProgress >= 100,
                "Learning Star - Reach 100% average progress"
        );

        int unlocked = 0;

        if (completedCourses >= 1) unlocked++;
        if (completedCourses >= 5) unlocked++;
        if (completedGoals >= 1) unlocked++;
        if (completedGoals >= 5) unlocked++;
        if (overallProgress >= 100) unlocked++;

        progressAchievements.setMax(5);
        progressAchievements.setProgress(unlocked);
    }

    private void setBadge(TextView view, boolean unlocked, String title) {
        if (unlocked) {
            view.setText("🏆 UNLOCKED: " + title);
            view.setTextColor(getResources().getColor(R.color.ltm_green));
        } else {
            view.setText("🔒 LOCKED: " + title);
            view.setTextColor(getResources().getColor(R.color.ltm_navy));
        }
    }

    private void loadStudyStreak() {
        String today = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());

        String lastStudyDate = streakPreferences.getString(
                "lastStudyDate", ""
        );

        studyStreak = streakPreferences.getInt("studyStreak", 0);

        if (!today.equals(lastStudyDate)
                && !isYesterday(lastStudyDate)
                && !lastStudyDate.isEmpty()) {

            studyStreak = 0;

            streakPreferences.edit()
                    .putInt("studyStreak", 0)
                    .apply();
        }

        tvStudyStreak.setText("🔥 Study streak: " + studyStreak + " days");
    }

    private boolean isYesterday(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        String yesterday = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(calendar.getTime());

        return yesterday.equals(dateString);
    }
}
