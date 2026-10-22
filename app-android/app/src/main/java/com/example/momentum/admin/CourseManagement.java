package com.example.momentum.admin;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.Course;
import com.example.momentum.service.AdminService;
import com.example.momentum.service.CourseService;

import java.util.List;

// Course Management: every course from LearnTech, with what it's worth.
// Courses themselves come from LearnTech and can't be created here, so the admin
// sets what we own: the points override and the expected minutes.
public class CourseManagement extends AdminScreen {

    // LearnTech's id for Fast Course, the demo course with a random 1 to 30 points.
    private static final int FAST_COURSE_ID = 58;

    private final CourseService courseService = new CourseService();
    private final AdminService adminService = new AdminService();

    @Override
    protected String title() {
        return "Course Management";
    }

    @Override
    protected String subtitle() {
        return "Points and expected time for each LearnTech course";
    }

    @Override
    protected void load() {
        content.removeAllViews();
        note("Loading courses...");

        courseService.getCatalogue(new ApiCallback<List<Course>>() {
            @Override
            public void onSuccess(List<Course> courses) {
                content.removeAllViews();
                content.addView(text("A completed course is worth its expected minutes in points, "
                        + "if the learner spent at least 70% of that time in it. "
                        + "Set your own points to override it. Tap a course to change it.", 13, MUTED, false));
                heading(courses.size() + (courses.size() == 1 ? " course" : " courses"));
                if (courses.isEmpty()) {
                    note("No courses yet. They appear after the first sync with LearnTech.");
                }
                for (Course course : courses) {
                    showCourse(course);
                }
            }

            @Override
            public void onError(String message) {
                content.removeAllViews();
                note(message);
            }
        });
    }

    // One course card: its name, expected time and what it's worth.
    private void showCourse(Course course) {
        LinearLayout box = card();
        box.addView(text(course.name, 15, NAVY, true));

        String minutes = course.expectedMinutes != null ? course.expectedMinutes + " min" : "no expected time";
        String points;
        if (course.platformCourseId == FAST_COURSE_ID) {
            points = "random 1-30 points (demo course)";
        } else if (course.pointsValue != null) {
            points = course.pointsValue + " points (set by admin)";
        } else if (course.expectedMinutes != null) {
            points = course.expectedMinutes + " points (from minutes)";
        } else {
            points = "10 points (no expected time)";
        }
        box.addView(text(minutes + "  •  " + points, 13, MUTED, false));
        box.setOnClickListener(v -> showEditForm(course));
    }

    // Change the points override and expected minutes. An empty box clears it.
    private void showEditForm(Course course) {
        EditText points = field("Points (empty = use the minutes)",
                course.pointsValue != null ? String.valueOf(course.pointsValue) : null, true);
        EditText minutes = field("Expected minutes (empty = not set)",
                course.expectedMinutes != null ? String.valueOf(course.expectedMinutes) : null, true);

        new AlertDialog.Builder(this)
                .setTitle(course.name)
                .setView(form(points, minutes))
                .setPositiveButton("Save", (dialog, which) -> {
                    Integer pointsValue = numberFrom(points);
                    Integer expectedMinutes = numberFrom(minutes);
                    if (expectedMinutes != null && expectedMinutes <= 0) {
                        toast("Expected minutes must be more than 0");
                        return;
                    }
                    adminService.updateCourse(course.courseId, pointsValue, expectedMinutes,
                            new ApiCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    toast("Course updated");
                                    load();
                                }

                                @Override
                                public void onError(String message) {
                                    toast(message);
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
