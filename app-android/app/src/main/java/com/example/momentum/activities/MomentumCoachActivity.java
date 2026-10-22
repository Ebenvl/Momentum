
package com.example.momentum.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.MainActivity;
import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.Course;
import com.example.momentum.model.Enrolment;
import com.example.momentum.model.Goal;
import com.example.momentum.service.CourseService;
import com.example.momentum.service.GoalService;

import java.util.ArrayList;
import java.util.List;

public class MomentumCoachActivity extends BaseActivity {

    private TextView tvCoachGreeting;
    private TextView tvCoachSummary;
    private TextView tvCoachCourses;
    private TextView tvCoachGoals;
    private TextView tvCoachRecommendation;

    private Button btnCoachAction;
    private Button btnRefreshCoach;

    private final CourseService courseService = new CourseService();
    private final GoalService goalService = new GoalService();
    private String nextAction = "goals";

    private final List<Enrolment> courses = new ArrayList<>();
    private final List<Goal> goals = new ArrayList<>();
    private final List<Course> catalogue = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_momentum_coach);

        tvCoachGreeting = findViewById(R.id.tvCoachGreeting);
        tvCoachSummary = findViewById(R.id.tvCoachSummary);
        tvCoachCourses = findViewById(R.id.tvCoachCourses);
        tvCoachGoals = findViewById(R.id.tvCoachGoals);
        tvCoachRecommendation = findViewById(R.id.tvCoachRecommendation);

        btnCoachAction = findViewById(R.id.btnCoachAction);
        btnRefreshCoach = findViewById(R.id.btnRefreshCoach);

        if (!Session.isLoggedIn()) {
            startActivity(new Intent(
                    MomentumCoachActivity.this,
                    LoginActivity.class
            ));
            finish();
            return;
        }

        tvCoachGreeting.setText("Let's make today count!");

        btnRefreshCoach.setOnClickListener(v -> loadCoachData());

        btnCoachAction.setOnClickListener(v -> {
            if ("courses".equals(nextAction)) {
                openTab("Courses");
            } else if ("achievements".equals(nextAction)) {
                openActivity(AchievementsActivity.class);
            } else {
                openTab("Goals");
            }
        });

        loadCoachData();
    }

    // Loads the catalogue (for course names), then the learner's courses, then their goals.
    private void loadCoachData() {
        btnRefreshCoach.setEnabled(false);
        tvCoachSummary.setText("Checking your learning progress...");
        tvCoachRecommendation.setText("Preparing your personal recommendation...");

        courses.clear();
        goals.clear();

        courseService.getCatalogue(new ApiCallback<List<Course>>() {
            @Override
            public void onSuccess(List<Course> result) {
                catalogue.clear();
                catalogue.addAll(result);
                loadCourses();
            }

            @Override
            public void onError(String error) {
                loadCourses();
            }
        });
    }

    private void loadCourses() {
        courseService.getMyCourses(new ApiCallback<List<Enrolment>>() {
            @Override
            public void onSuccess(List<Enrolment> result) {
                courses.addAll(result);

                goalService.getGoals(new ApiCallback<List<Goal>>() {
                    @Override
                    public void onSuccess(List<Goal> goalResult) {
                        for (Goal goal : goalResult) {
                            if (!"cancelled".equals(goal.status)) {
                                goals.add(goal);
                            }
                        }
                        updateCoach();
                        btnRefreshCoach.setEnabled(true);
                    }

                    @Override
                    public void onError(String error) {
                        btnRefreshCoach.setEnabled(true);

                        tvCoachSummary.setText(
                                "I couldn't load your goals. Please try again."
                        );

                        tvCoachRecommendation.setText(
                                "Refresh your recommendations to try again."
                        );

                        Toast.makeText(
                                MomentumCoachActivity.this,
                                "Could not load goals: " + error,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                btnRefreshCoach.setEnabled(true);

                tvCoachSummary.setText(
                        "I couldn't load your courses. Please try again."
                );

                tvCoachRecommendation.setText(
                        "Refresh your recommendations to try again."
                );

                Toast.makeText(
                        MomentumCoachActivity.this,
                        "Could not load courses: " + error,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void updateCoach() {
        int completedCourses = 0;
        int completedGoals = 0;

        Enrolment courseToFocusOn = null;
        int lowestProgress = 101;

        for (Enrolment course : courses) {
            int progress = getCourseProgress(course);

            if (isCourseCompleted(course)) {
                completedCourses++;
            } else if (progress < lowestProgress) {
                lowestProgress = progress;
                courseToFocusOn = course;
            }
        }

        Goal goalToFocusOn = null;

        for (Goal goal : goals) {
            if (isGoalCompleted(goal)) {
                completedGoals++;
            } else if (goalToFocusOn == null) {
                goalToFocusOn = goal;
            }
        }

        tvCoachCourses.setText(String.valueOf(completedCourses));
        tvCoachGoals.setText(String.valueOf(completedGoals));

        tvCoachSummary.setText(
                "You have completed " + completedCourses + " of "
                        + courses.size() + " courses and "
                        + completedGoals + " of " + goals.size()
                        + " goals. Every small step counts!"
        );

        if (goalToFocusOn != null) {
            String goalTitle = getTitle(goalToFocusOn.title, "your next goal");

            tvCoachRecommendation.setText(
                    "Your next step is to work on \"" + goalTitle
                            + "\". Break it into one small task and make "
                            + "progress today."
            );

            btnCoachAction.setText("Work on my goals");
            nextAction = "goals";

        } else if (courseToFocusOn != null) {
            String courseTitle = getTitle(
                    courseName(courseToFocusOn.courseId),
                    "your next course"
            );

            // LearnTech only knows if a course is not started, in progress or done
            String where = "in_progress".equals(courseToFocusOn.state)
                    ? "which you have already started"
                    : "which you haven't started yet";

            tvCoachRecommendation.setText(
                    "Focus on \"" + courseTitle + "\", " + where
                            + ". Spend some time learning on LearnTech today."
            );

            btnCoachAction.setText("Continue learning");
            nextAction = "courses";

        } else if (!courses.isEmpty() || !goals.isEmpty()) {
            tvCoachGreeting.setText("Amazing work!");

            tvCoachRecommendation.setText(
                    "You've completed all the courses and goals "
                            + "currently recorded as complete. Celebrate "
                            + "your progress and choose your next learning goal."
            );

            btnCoachAction.setText("View my achievements");
            nextAction = "achievements";

        } else {
            tvCoachRecommendation.setText(
                    "Start by adding a learning goal. "
                            + "Once you do, I can recommend your next step."
            );

            btnCoachAction.setText("Create a goal");
            nextAction = "goals";
        }
    }

    // Not started, in progress and completed, as 0, 50 and 100, only used to
    // pick which course to focus on first.
    private int getCourseProgress(Enrolment course) {
        if ("completed".equals(course.state)) {
            return 100;
        }
        if ("in_progress".equals(course.state)) {
            return 50;
        }
        return 0;
    }

    private boolean isCourseCompleted(Enrolment course) {
        return "completed".equals(course.state);
    }

    private boolean isGoalCompleted(Goal goal) {
        return goal.status != null
                && goal.status.equalsIgnoreCase("completed");
    }

    private String courseName(int courseId) {
        for (Course course : catalogue) {
            if (course.courseId == courseId) {
                return course.name;
            }
        }
        return null;
    }

    private String getTitle(
            String title,
            String fallback
    ) {
        if (title == null || title.trim().isEmpty()) {
            return fallback;
        }

        return title;
    }

    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(
                MomentumCoachActivity.this,
                activityClass
        );

        startActivity(intent);
    }

    // Opens one of the main tabs, like Courses or Goals.
    private void openTab(String tab) {
        Intent intent = new Intent(
                MomentumCoachActivity.this,
                MainActivity.class
        );
        intent.putExtra("tab", tab);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
        finish();
    }
}
