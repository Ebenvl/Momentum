package com.example.momentum;

import com.example.momentum.activities.BaseActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.content.Intent;
import android.text.InputType;
import com.example.momentum.activities.AchievementsActivity;
import com.example.momentum.activities.CertificateActivity;
import com.example.momentum.activities.LoginActivity;
import com.example.momentum.activities.MomentumCoachActivity;
import com.example.momentum.activities.MomentumMapActivity;
import com.example.momentum.activities.RemindersActivity;
import com.example.momentum.activities.SignupActivity;
import com.example.momentum.activities.StudyCoachActivity;
import com.example.momentum.activities.StudyTimetableActivity;
import com.example.momentum.admin.AdminPanel;
import com.example.momentum.model.Enrolment;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.AppSettings;
import com.example.momentum.data.EasterEggs;
import com.example.momentum.data.Session;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.ProgressSnapshot;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;
import com.example.momentum.service.CourseService;
import com.example.momentum.service.GoalService;
import com.example.momentum.service.LeaderboardService;
import com.example.momentum.service.ProfileService;
import com.example.momentum.activities.LearnTechActivity;
import com.example.momentum.activities.SettingsActivity;
import com.example.momentum.model.CheckIn;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends BaseActivity {

   // colors used throughout the app
    private static final int NAVY = Color.rgb(7, 59, 76);
    private static final int DEEP_NAVY = Color.rgb(7, 29, 53);
    private static final int PURPLE = Color.rgb(118, 102, 232);
    private static final int TEAL = Color.rgb(53, 184, 165);
    private static final int TEAL_DARK = Color.rgb(14, 140, 124);
    private static final int TEAL_BG = Color.rgb(222, 246, 241);
    private static final int GOLD = Color.rgb(244, 185, 66);
    private static final int ORANGE = Color.rgb(228, 108, 40);
    private static final int ORANGE_BG = Color.rgb(253, 235, 224);
    private static final int MUTED = Color.rgb(104, 119, 131);
    private static final int PALE = Color.rgb(244, 246, 250);
    private static final int TRACK = Color.rgb(225, 230, 240);
    private static final int SOFT_PURPLE = Color.rgb(237, 234, 255);
    private static final int WHITE = Color.WHITE;

    private static final int TAB_HOME = 1, TAB_COURSES = 2, TAB_GOALS = 3,
            TAB_LEADERBOARD = 4, TAB_PROFILE = 5;

    // Statuses
    private FrameLayout pageContainer;
    private BottomNavigationView bottomNavigation;
    private String currentTab = "Home";
    private String currentScreen = "main";
    private String courseFilter = "All";
    private String searchQuery = "";
    private String goalFilter = "All";
    private String goalSearch = "";
    private String selectedPeriod = "Monthly";
    private final ArrayList<Course> courses = new ArrayList<>();
    private final ArrayList<Goal> goals = new ArrayList<>();
    private final ArrayList<LeaderEntry> leaderboard = new ArrayList<>();
    private final ArrayList<ProgressSnapshot> progress = new ArrayList<>();
    private String userName = "";
    private int userXp = 0;
    private int streakDays = 0;

    // Backend connection
    private final AuthService authService = new AuthService();
    private final GoalService goalService = new GoalService();
    private final CourseService courseService = new CourseService();
    private final LeaderboardService leaderboardService = new LeaderboardService();
    private final ArrayList<com.example.momentum.model.Course> catalogue = new ArrayList<>();
    private int loadedUserId = -1;


    // Catalogue courses the learner isn't enrolled on, and whether their account is linked.
    private final ArrayList<Course> otherCourses = new ArrayList<>();
    private Boolean linked;   // null until /me answers
    private final ProfileService profileService = new ProfileService();

    // The learning platform, where "Continue Learning" takes the learner.
    private static final String LMS_URL = "https://eduvos.learntech.co.za/";

    // LearnTech's id for Fast Course, the demo course. It gives a random 1 to 30 points.
    private static final int FAST_COURSE_ID = 58;

    // Dark mode: the light cards and dark text swap to dark cards and light text.
    // Done in the colour helpers (rounded and text), so every screen follows.
    private boolean dark;

    // Taps on the Home logo, for the Curious Mind easter egg.
    private int logoTaps = 0;

    // The text size this screen was drawn with, to notice a change in Settings.
    private float appliedTextSize;
    private static final int DARK_CARD = Color.rgb(27, 42, 63);
    private static final int DARK_SOFT = Color.rgb(38, 56, 82);
    private static final int DARK_TEXT = Color.rgb(232, 237, 245);
    private static final int DARK_MUTED = Color.rgb(169, 182, 194);

    // From /me: points from every month, and the longest streak ever.
    // userXp above is this month's points, from the leaderboard.
    private int totalXp = 0;
    private int longestStreak = 0;

    private static class Course {
        String title = "", status = "New";
        String completedOn = "";
        int xp;


        // From LearnTech, for the course list and the course page.
        int courseId;                  // our own id, from the catalogue
        String xpText = "";            // like "45 XP", or "1-30 XP" for Fast Course
        Integer expectedMinutes;       // null if LearnTech has no duration for it
        int timeSpentMinutes;
        boolean notEnrolled;           // in the catalogue, but the learner isn't on it
    }

    private static class Goal {
        int id;
        String title;
        String course = "", target = "", lastAccessed = "", completedDate = "";
        String status = "On Track";
        boolean completed;


        // From the backend, for editing and for showing what the goal is.
        String goalType = "free_text";
        Integer targetValue, targetCourseId;
        String targetDate;            // yyyy-MM-dd, null if there's no deadline
        boolean missed;               // the target date passed before it was reached

        Goal(int id, String title) { this.id = id; this.title = title; }
    }

    private static class LeaderEntry {
        String name = "";
        int xp;
        boolean you;
    }

    private static class Page {
        FrameLayout root;
        LinearLayout content;
        Page(FrameLayout root, LinearLayout content) {
            this.root = root;
            this.content = content;
        }
    }


    // The app'sLifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Dark mode, from Settings or the phone's own setting
        dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);


        // Stops Android drawing its own see-through strip behind the home and back
        // buttons, so the app's bottom bar colour shows there, light or dark.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        appliedTextSize = AppSettings.getTextSize(this);

        // The splash only shows when the app opens, not when a screen is redrawn
        // after a theme change.
        if (savedInstanceState != null) {
            buildAppShell();
            showStudentTab("Home");
            loadUserIfNew();
        } else {
            showSplash();

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                buildAppShell();
                showStudentTab("Home");
                loadUserIfNew();
            }, 1600);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                switch (currentScreen) {
                    case "courseDetails": showStudentTab("Courses"); break;
                    default:
                        if (!"Home".equals(currentTab)) showStudentTab("Home");
                        else finish();
                }
            }
        });
    }

    // Coming back from the login or sign-up screen, or another screen.
    // Loads the learner's data once they are logged in.
    @Override
    protected void onResume() {
        super.onResume();


        // The text size changed in Settings: redraw this screen with it.
        if (appliedTextSize != AppSettings.getTextSize(this)) {
            recreate();
            return;
        }

        if (bottomNavigation == null) return;
        loadUserIfNew();
    }


    // Loads the logged-in learner's data, once per login. Also runs right after
    // the splash, for a user who was still logged in from last time.
    private void loadUserIfNew() {
        if (Session.isLoggedIn() && Session.getUser().id != loadedUserId) {
            loadedUserId = Session.getUser().id;
            userName = Session.getUser().fullName;
            streakDays = 0;   // the real streak comes from the backend in loadMe
            loadData();
            showStudentTab("Home");
        }
    }

    // Other screens can ask to open one of the tabs, like Courses or Goals.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String tab = intent.getStringExtra("tab");
        if (tab != null && bottomNavigation != null) showStudentTab(tab);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private void setSystemBarIcons(boolean lightBackground) {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(lightBackground);
        c.setAppearanceLightNavigationBars(!dark);
    }

    private void showSplash() {
        setSystemBarIcons(true);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(WHITE);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.learntech_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150), Gravity.CENTER);
        lp.setMargins(dp(28), 0, dp(28), 0);
        frame.addView(logo, lp);
        setContentView(frame);
    }


    // bottom navigation and app's shell

    private void buildAppShell() {
        setSystemBarIcons(false);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(NAVY);

        pageContainer = new FrameLayout(this);
        shell.addView(pageContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        bottomNavigation = new BottomNavigationView(this);
        bottomNavigation.setBackgroundColor(darkBackground(WHITE));
        bottomNavigation.setElevation(dp(8));
        bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        bottomNavigation.setItemHorizontalTranslationEnabled(false);
        bottomNavigation.setItemActiveIndicatorEnabled(false);

        Menu menu = bottomNavigation.getMenu();
        menu.add(Menu.NONE, TAB_HOME, 0, "Home").setIcon(new Icon("home", 0));
        menu.add(Menu.NONE, TAB_COURSES, 1, "Courses").setIcon(new Icon("book", 0));
        menu.add(Menu.NONE, TAB_GOALS, 2, "Goals").setIcon(new Icon("target", 0));
        menu.add(Menu.NONE, TAB_LEADERBOARD, 3, "Leaderboard").setIcon(new Icon("trophy", 0));
        menu.add(Menu.NONE, TAB_PROFILE, 4, "Profile").setIcon(new Icon("person", 0));

        ColorStateList navColors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{PURPLE, MUTED});
        bottomNavigation.setItemIconTintList(navColors);
        bottomNavigation.setItemTextColor(navColors);
        bottomNavigation.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case TAB_HOME: showStudentTab("Home"); return true;
                case TAB_COURSES: showStudentTab("Courses"); return true;
                case TAB_GOALS: showStudentTab("Goals"); return true;
                case TAB_LEADERBOARD: refreshIfLoggedIn(); showStudentTab("Leaderboard"); return true;
                case TAB_PROFILE: refreshIfLoggedIn(); showStudentTab("Profile"); return true;
                default: return false;
            }
        });

        shell.addView(bottomNavigation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(shell);
    }

    private void checkTab(int id) {
        bottomNavigation.getMenu().findItem(id).setChecked(true);
    }

    private void showStudentTab(String tab) {
        currentTab = tab;
        currentScreen = "main";
        bottomNavigation.setVisibility(View.VISIBLE);
        int id = TAB_HOME;
        if ("Courses".equals(tab)) id = TAB_COURSES;
        else if ("Goals".equals(tab)) id = TAB_GOALS;
        else if ("Leaderboard".equals(tab)) id = TAB_LEADERBOARD;
        else if ("Profile".equals(tab)) id = TAB_PROFILE;
        checkTab(id);

        switch (tab) {
            case "Courses": showCourses(); break;
            case "Goals": showGoals(); break;
            case "Leaderboard": showLeaderboard(); break;
            case "Profile": showProfile(); break;
            default: currentTab = "Home"; showHome(); break;
        }
    }

    private void showPage(View page, boolean hideNav) {
        pageContainer.removeAllViews();
        pageContainer.addView(page, new FrameLayout.LayoutParams(-1, -1));
        bottomNavigation.setVisibility(hideNav ? View.GONE : View.VISIBLE);
    }


    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(darkBackground(color));
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    // Design elements and formating (bg image, logo inserts and text)

    private Page createPage(String title, String subtitle, boolean showLogo) {
        FrameLayout root = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.app_bg);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), statusBarHeight() + dp(20), dp(20), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        if (title != null) {
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout titles = new LinearLayout(this);
            titles.setOrientation(LinearLayout.VERTICAL);
            titles.addView(text(title, WHITE, 26, true));
            if (subtitle != null && !subtitle.isEmpty()) {
                TextView sub = text(subtitle, Color.argb(225, 255, 255, 255), 14, false);
                sub.setPadding(0, dp(2), 0, 0);
                titles.addView(sub);
            }
            header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

            if (showLogo) {
                ImageView logo = new ImageView(this);
                logo.setImageResource(R.drawable.minimalist_logo);
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                GradientDrawable ring = oval(WHITE);
                ring.setStroke(dp(3), GOLD);
                logo.setBackground(ring);
                logo.setPadding(dp(8), dp(8), dp(8), dp(8));
                LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(56), dp(56));
                logoLp.leftMargin = dp(12);
                header.addView(logo, logoLp);


                // Easter egg: tapping the logo 7 times unlocks a secret badge
                logo.setOnClickListener(v -> {
                    logoTaps++;
                    if (logoTaps == 7) {
                        logoTaps = 0;
                        EasterEggs.report(this, EasterEggs.LOGO_TAPPED);
                    }
                });
            }
            content.addView(header, new LinearLayout.LayoutParams(-1, -2));
            content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(18)));
        }
        return new Page(root, content);
    }

    private TextView text(String value, int color, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(darkText(color));
        t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout cardIn(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(rounded(WHITE, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        parent.addView(c, lp);
        return c;
    }

    private LinearLayout card(Page page) {
        return cardIn(page.content);
    }

    private void addText(LinearLayout parent, String value, int color, float size, boolean bold) {
        TextView t = text(value, color, size, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(7);
        parent.addView(t, lp);
    }

    private void addHeading(LinearLayout parent, String value) {
        TextView t = text(value, WHITE, 18, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(10);
        parent.addView(t, lp);
    }

    // In dark mode, light card colours become dark ones. Anything else stays as it is.
    private int darkBackground(int color) {
        if (!dark) return color;
        if (color == WHITE || color == PALE) return DARK_CARD;
        if (color == Color.rgb(226, 231, 241) || color == Color.rgb(232, 236, 255)) return DARK_SOFT;
        return color;
    }

    // In dark mode, the dark navy and grey text become light, so they show on dark cards.
    private int darkText(int color) {
        if (!dark) return color;
        if (color == DEEP_NAVY) return DARK_TEXT;
        if (color == MUTED) return DARK_MUTED;
        return color;
    }

    private TextView pill(String label, int fg, int bg) {
        TextView t = text(label, fg, 11, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(4), dp(10), dp(4));
        t.setBackground(rounded(bg, 12));
        return t;
    }

    private Button button(String label, boolean outline, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(null, Typeface.BOLD);
        b.setMinHeight(dp(46));
        b.setInputType(0);

        b.setStateListAnimator(null);
        b.setPadding(dp(16), dp(5), dp(16), dp(5));
        if (outline) {
            GradientDrawable d = rounded(WHITE, 28);
            d.setStroke(dp(1), PURPLE);
            b.setBackground(d);
            b.setTextColor(PURPLE);
        } else {
            b.setBackground(rounded(PURPLE, 28));
            b.setTextColor(WHITE);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.bottomMargin = dp(8);
        b.setOnClickListener(listener);
        b.setTag(lp);
        return b;
    }

    private void addButton(LinearLayout parent, String label, boolean outline, View.OnClickListener listener) {
        Button b = button(label, outline, listener);
        parent.addView(b, (LinearLayout.LayoutParams) b.getTag());
    }

    private ImageView iconView(String type, int color, int sizeDp) {
        ImageView v = new ImageView(this);
        v.setImageDrawable(new Icon(type, color));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return v;
    }

    private TextView makeChip(String label, boolean selected) {
        TextView chip = text(label, selected ? WHITE : DEEP_NAVY, 13, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(18), dp(8), dp(18), dp(8));
        chip.setBackground(rounded(selected ? PURPLE : WHITE, 24));
        return chip;
    }

    private void addBack(LinearLayout parent, String label, View.OnClickListener listener) {
        TextView back = text("\u2190  " + label, WHITE, 15, true);
        back.setPadding(0, dp(4), 0, dp(14));
        back.setOnClickListener(listener);
        parent.addView(back);
    }

    private void message(String title, String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body)
                .setPositiveButton("OK", (d, w) -> {}).show();
    }

    private String today() {
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date());
    }

   // Homepage
    private void showHome() {
        Page p = createPage(userName.isEmpty() ? "Welcome back" : "Welcome back, " + userName,
                "Let's build some new skills today!", true);

        int completedCourses = 0;
        Course inProgress = null;
        for (Course c : courses) {
            if ("Completed".equals(c.status)) completedCourses++;
            if (inProgress == null && "In Progress".equals(c.status)) inProgress = c;
        }
        int goalsMet = 0;
        for (Goal g : goals) if (g.completed) goalsMet++;

        // Progress card and status
        LinearLayout progressCard = card(p);
        progressCard.setOrientation(LinearLayout.HORIZONTAL);
        progressCard.setGravity(Gravity.CENTER_VERTICAL);



        // Overall progress: courses completed out of courses enrolled. The only
        // percentage LearnTech can give, since single courses have no progress.
        float progress = courses.isEmpty() ? 0f : completedCourses / (float) courses.size();
        progressCard.addView(new RingView(this, progress), new LinearLayout.LayoutParams(dp(80), dp(80)));
        String progressMsg = courses.isEmpty()
                ? "Your progress shows here once your LearnTech courses come through."
                : completedCourses + " of " + courses.size() + " courses complete, "
                + Math.round(progress * 100) + "%.";
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(0, -2, 1f);
        msgLp.leftMargin = dp(16);
        progressCard.addView(text(progressMsg, MUTED, 14, false), msgLp);


        addHeading(p.content, "Continue Learning");
        LinearLayout cont = card(p);
        if (inProgress == null) {
            addText(cont, "No course in progress", DEEP_NAVY, 17, true);
            addText(cont, "Courses you start will appear here so you can pick up where you left off.", MUTED, 14, false);
            addButton(cont, "Browse courses", false, v -> showStudentTab("Courses"));
        } else {
            final Course current = inProgress;
            View banner = new View(this);
            banner.setBackground(rounded(DEEP_NAVY, 12));
            LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(-1, dp(120));
            bannerLp.bottomMargin = dp(14);
            cont.addView(banner, bannerLp);
            addText(cont, current.title.toUpperCase(Locale.getDefault()), PURPLE, 12, true);
            addText(cont, "In progress" + (current.expectedMinutes != null
                    ? "  •  " + current.expectedMinutes + " min course" : ""), MUTED, 13, false);
            cont.setOnClickListener(v -> showCourseDetails(current));
        }

        // Ativity staus
        addHeading(p.content, "Your Activity");
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statCard("book", PURPLE, String.valueOf(courses.size()), "ENROLLED", true), statLp(false));
        stats.addView(statCard("target", TEAL, String.valueOf(goalsMet), "GOALS MET", true), statLp(false));
        stats.addView(statCard("hex", GOLD, String.format(Locale.getDefault(), "%,d", totalXp), "XP POINTS", false), statLp(true));
        p.content.addView(stats, new LinearLayout.LayoutParams(-1, -2));

        // The extra screens
        p.content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(18)));
        addHeading(p.content, "More For You");
        LinearLayout more = card(p);


        // The coaches suggest what to do next, so they stay on Home. Achievements and
        // the Momentum Map moved to Profile, the timetable to Goals, reminders to Settings.
        addButton(more, "Momentum Coach", false, v -> openScreen(MomentumCoachActivity.class));
        addButton(more, "Study Coach", true, v -> openScreen(StudyCoachActivity.class));

        showPage(p.root, false);
    }

    // Opens one of the extra screens. Most of them need the learner to be logged in.
    private void openScreen(Class<?> screen) {
        if (!Session.isLoggedIn() && screen != StudyCoachActivity.class && screen != RemindersActivity.class) {
            Toast.makeText(this, "Log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }


        // For the Master Planner easter egg: the timetable and a coach on the same day
        if (screen == StudyTimetableActivity.class) {
            EasterEggs.report(this, EasterEggs.OPENED_TIMETABLE);
        } else if (screen == MomentumCoachActivity.class || screen == StudyCoachActivity.class) {
            EasterEggs.report(this, EasterEggs.OPENED_COACH);
        }

        startActivity(new Intent(this, screen));
    }

    private LinearLayout.LayoutParams statLp(boolean last) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        if (!last) lp.rightMargin = dp(10);
        return lp;
    }

    private LinearLayout statCard(String icon, int color, String value, String label, boolean unused) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(8), dp(16), dp(8), dp(16));
        c.setBackground(rounded(WHITE, 16));
        c.addView(iconView(icon, color, 26));
        TextView v = text(value, DEEP_NAVY, 20, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(8), 0, dp(2));
        c.addView(v, new LinearLayout.LayoutParams(-1, -2));
        TextView l = text(label, MUTED, 11, false);
        l.setGravity(Gravity.CENTER);
        l.setLetterSpacing(0.06f);
        c.addView(l, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }


    // Courses

    private void showCourses() {
        Page p = createPage("My Courses", "Manage and track your enrolled learning paths", false);

        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(18), 0, dp(14), 0);
        searchBox.setBackground(rounded(WHITE, 28));
        ImageView searchIcon = iconView("search", MUTED, 22);
        searchBox.addView(searchIcon);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(darkText(DEEP_NAVY));
        search.setHintTextColor(darkText(MUTED));
        search.setHint("Search courses, topics, skills...");
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(12), 0, 0, 0);
        search.setText(searchQuery);
        LinearLayout.LayoutParams searchFieldLp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        searchBox.addView(search, searchFieldLp);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(52));
        searchLp.bottomMargin = dp(14);
        p.content.addView(searchBox, searchLp);

        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        for (String option : new String[]{"All", "In Progress", "Completed", "New", "Not Enrolled"}) {
            TextView chip = makeChip(option, courseFilter.equals(option));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(38));
            lp.rightMargin = dp(8);
            filters.addView(chip, lp);
            chip.setOnClickListener(v -> { courseFilter = option; showCourses(); });
        }
        chipScroll.addView(filters);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-1, dp(42));
        chipLp.bottomMargin = dp(14);
        p.content.addView(chipScroll, chipLp);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        p.content.addView(list, new LinearLayout.LayoutParams(-1, -2));
        renderCourseList(list);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                renderCourseList(list);
            }
            public void afterTextChanged(Editable s) {}
        });
        showPage(p.root, false);
    }

    private void renderCourseList(LinearLayout list) {
        list.removeAllViews();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        int shown = 0;


        // An account that isn't linked to a LearnTech learner has no courses of its own yet.
        if (Boolean.FALSE.equals(linked)) {
            LinearLayout note = cardIn(list);
            addText(note, "Not linked to LearnTech yet", DEEP_NAVY, 16, true);
            addText(note, "Your account isn't linked to a learner on the learning platform, so your own "
                    + "courses can't show yet. Use the same email as on the platform, or ask an "
                    + "administrator to link your account.", MUTED, 14, false);
        }

        for (Course c : courses) {
            if (!"All".equals(courseFilter) && !courseFilter.equals(c.status)) continue;
            if (!q.isEmpty() && !c.title.toLowerCase(Locale.ROOT).contains(q)) continue;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(12);
            list.addView(courseRow(c), lp);
            shown++;
        }


        // The rest of the catalogue, under its own heading.
        if ("All".equals(courseFilter) || "Not Enrolled".equals(courseFilter)) {
            boolean headingAdded = false;
            for (Course c : otherCourses) {
                if (!q.isEmpty() && !c.title.toLowerCase(Locale.ROOT).contains(q)) continue;
                if (!headingAdded) {
                    addHeading(list, "Other courses");
                    headingAdded = true;
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(12);
                list.addView(courseRow(c), lp);
                shown++;
            }
        }

        if (shown == 0) {
            LinearLayout empty = cardIn(list);
            boolean none = courses.isEmpty();
            addText(empty, none ? "No courses available yet" : "No courses found", DEEP_NAVY, 17, true);
            addText(empty, none ? "Your courses show here once they come through from LearnTech."
                    : "Try a different search or filter.", MUTED, 14, false);
        }
    }

    private View courseRow(Course c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(rounded(WHITE, 16));

        ImageView thumb = new ImageView(this);
        thumb.setBackground(rounded(DEEP_NAVY, 12));
        row.addView(thumb, new LinearLayout.LayoutParams(dp(76), dp(76)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(c.title, DEEP_NAVY, 15, true));


        // The course's state under its name, since LearnTech has no instructors.
        String subtitle = c.notEnrolled ? "NOT ENROLLED"
                : "New".equals(c.status) ? "NOT STARTED"
                : c.status.toUpperCase(Locale.getDefault());
        TextView inst = text(subtitle, MUTED, 11, true);
        inst.setPadding(0, dp(4), 0, 0);
        info.addView(inst);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
        infoLp.leftMargin = dp(12);
        row.addView(info, infoLp);

        TextView xp = pill(c.xpText.isEmpty() ? c.xp + " XP" : c.xpText, TEAL_DARK, TEAL_BG);
        LinearLayout.LayoutParams xpLp = new LinearLayout.LayoutParams(-2, -2);
        xpLp.leftMargin = dp(8);
        row.addView(xp, xpLp);

        row.setOnClickListener(v -> showCourseDetails(c));
        return row;
    }


    // Goals setter and goal tracker

    private void showGoals() {
        Page p = createPage("My Goals", "Stay accountable with personalized learning milestones", false);


        // The weekly study timetable sits with the goals, since both are about planning.
        LinearLayout plan = card(p);
        addText(plan, "Plan when you'll study each week, and stick to it.", MUTED, 13, false);
        addButton(plan, "Study timetable", false, v -> openScreen(StudyTimetableActivity.class));

        if (goals.isEmpty()) {
            LinearLayout empty = card(p);
            addText(empty, "No goals added yet", DEEP_NAVY, 17, true);
            addText(empty, "Tap the + button to add your first learning goal.", MUTED, 14, false);
        }


        // Search and filter, like on the Courses tab. Only the list below is
        // redrawn while typing, so the keyboard stays open.
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        if (!goals.isEmpty()) {
            LinearLayout searchBox = new LinearLayout(this);
            searchBox.setOrientation(LinearLayout.HORIZONTAL);
            searchBox.setGravity(Gravity.CENTER_VERTICAL);
            searchBox.setPadding(dp(18), 0, dp(14), 0);
            searchBox.setBackground(rounded(WHITE, 28));
            searchBox.addView(iconView("search", MUTED, 22));
            EditText search = new EditText(this);
            search.setSingleLine(true);
            search.setTextSize(14);
            search.setTextColor(darkText(DEEP_NAVY));
            search.setHintTextColor(darkText(MUTED));
            search.setHint("Search goals or courses...");
            search.setBackgroundColor(Color.TRANSPARENT);
            search.setPadding(dp(12), 0, 0, 0);
            search.setText(goalSearch);
            searchBox.addView(search, new LinearLayout.LayoutParams(0, dp(52), 1f));
            LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(52));
            searchLp.bottomMargin = dp(14);
            p.content.addView(searchBox, searchLp);

            HorizontalScrollView chipScroll = new HorizontalScrollView(this);
            chipScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout filters = new LinearLayout(this);
            filters.setOrientation(LinearLayout.HORIZONTAL);
            for (String option : new String[]{"All", "Active", "Behind", "Completed"}) {
                TextView chip = makeChip(option, goalFilter.equals(option));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(38));
                lp.rightMargin = dp(8);
                filters.addView(chip, lp);
                chip.setOnClickListener(v -> { goalFilter = option; showGoals(); });
            }
            chipScroll.addView(filters);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-1, dp(42));
            chipLp.bottomMargin = dp(14);
            p.content.addView(chipScroll, chipLp);

            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    goalSearch = s.toString();
                    renderGoalList(list);
                }
                public void afterTextChanged(Editable s) {}
            });
        }
        p.content.addView(list, new LinearLayout.LayoutParams(-1, -2));
        renderGoalList(list);

        // Floating add button +

        p.content.addView(new View( this), new LinearLayout.LayoutParams(1, dp(72)));

        FrameLayout fab = new FrameLayout(this);
        GradientDrawable fabBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{PURPLE, Color.rgb(59, 130, 246)});
        fabBg.setShape(GradientDrawable.OVAL);
        fab.setBackground(fabBg);
        fab.setElevation(dp(6));
        fab.setContentDescription("Add goal");
        ImageView plus = new ImageView(this);
        plus.setImageDrawable(new Icon("plus", WHITE));
        plus.setPadding(dp(16), dp(16), dp(16), dp(16));
        fab.addView(plus, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
        fabLp.rightMargin = dp(20);
        fabLp.bottomMargin = dp(20);
        p.root.addView(fab, fabLp);
        fab.setOnClickListener(v -> showGoalForm(null));

        showPage(p.root, false);
    }


    // Does this goal match the search text and the chosen filter chip?
    private boolean goalMatches(Goal g) {
        String q = goalSearch.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty() && !g.title.toLowerCase(Locale.ROOT).contains(q)
                && !g.course.toLowerCase(Locale.ROOT).contains(q)) {
            return false;
        }
        boolean behind = "Behind".equals(g.status) || "Missed".equals(g.status);
        switch (goalFilter) {
            case "Active": return !g.completed;
            case "Behind": return !g.completed && behind;
            case "Completed": return g.completed;
            default: return true;
        }
    }

    // The goal cards: active ones first, then the completed ones.
    private void renderGoalList(LinearLayout list) {
        list.removeAllViews();
        ArrayList<Goal> active = new ArrayList<>();
        ArrayList<Goal> done = new ArrayList<>();
        for (Goal g : goals) {
            if (!goalMatches(g)) continue;
            if (g.completed) done.add(g); else active.add(g);
        }

        if (!goals.isEmpty() && active.isEmpty() && done.isEmpty()) {
            LinearLayout none = cardIn(list);
            addText(none, "No goals match", DEEP_NAVY, 16, true);
            addText(none, "Try other words, or another filter.", MUTED, 14, false);
        }

        for (Goal g : active) {
            LinearLayout c = cardIn(list);
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setPadding(0, 0, 0, dp(6));
            top.addView(text(g.title, DEEP_NAVY, 16, true), new LinearLayout.LayoutParams(0, -2, 1f));
            boolean behind = "Behind".equals(g.status) || "Missed".equals(g.status);
            LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(-2, -2);
            pillLp.leftMargin = dp(8);
            top.addView(pill(g.status, behind ? ORANGE : TEAL_DARK, behind ? ORANGE_BG : TEAL_BG), pillLp);
            c.addView(top, new LinearLayout.LayoutParams(-1, -2));
            if (!g.course.isEmpty()) addText(c, "Course: " + g.course, MUTED, 13, false);


            // What the goal is, for the types the app checks by itself.
            String what = goalSummary(g);
            if (!what.isEmpty()) addText(c, what, MUTED, 13, false);

            if (!g.target.isEmpty()) addText(c, "Target: " + g.target, DEEP_NAVY, 12, false);
            if (!g.lastAccessed.isEmpty()) addText(c, "Last accessed: " + g.lastAccessed, DEEP_NAVY, 12, false);
            c.setOnClickListener(v -> showGoalOptions(g));
        }

        if (!done.isEmpty()) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.argb(80, 255, 255, 255));
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(-1, dp(1));
            dLp.topMargin = dp(6);
            dLp.bottomMargin = dp(14);
            list.addView(divider, dLp);
            addHeading(list, "Completed Goals");

            for (Goal g : done) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(14), dp(14), dp(14), dp(14));
                row.setBackground(rounded(WHITE, 16));

                ImageView badge = iconView("check", TEAL, 40);
                badge.setBackground(oval(TEAL_BG));
                badge.setPadding(dp(10), dp(10), dp(10), dp(10));
                row.addView(badge, new LinearLayout.LayoutParams(dp(40), dp(40)));

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.addView(text(g.title, DEEP_NAVY, 14, true));
                if (!g.completedDate.isEmpty()) {
                    TextView when = text("Completed " + g.completedDate, MUTED, 12, false);
                    when.setPadding(0, dp(2), 0, 0);
                    info.addView(when);
                }
                LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
                infoLp.leftMargin = dp(12);
                row.addView(info, infoLp);

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = dp(10);
                list.addView(row, rowLp);
                row.setOnClickListener(v -> showGoalOptions(g));
            }
        }
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        return e;
    }


    // The goal types a learner can pick, and how they're shown in the form.
    // Every type except a personal goal is checked and completed by the app itself.
    private static final String[] GOAL_TYPES = {"free_text", "complete_specific_course",
            "complete_n_courses", "spend_minutes", "active_days"};
    private static final String[] GOAL_TYPE_NAMES = {"Personal goal (you tick it off)",
            "Finish a specific course", "Finish a number of courses",
            "Spend minutes learning", "Be active a number of days in a row"};

    // The form for adding a goal, or for editing one when a goal is passed in.
    private void showGoalForm(Goal existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        EditText title = field("Goal title");
        Spinner type = new Spinner(this);
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, GOAL_TYPE_NAMES));
        EditText course = field("Course name");
        EditText number = field("Target number");
        number.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText target = field("Target date, e.g. Dec 15 (optional)");
        form.addView(title);
        form.addView(type);
        form.addView(course);
        form.addView(number);
        form.addView(target);

        // Editing: fill in what the goal already has
        if (existing != null) {
            title.setText(existing.title);
            type.setSelection(Math.max(0, Arrays.asList(GOAL_TYPES).indexOf(existing.goalType)));
            course.setText(existing.course);
            if (existing.targetValue != null) number.setText(String.valueOf(existing.targetValue));
            target.setText(existing.target);
        }

        // Only show the course or number field the chosen type needs
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String goalType = GOAL_TYPES[position];
                boolean needsNumber = !"free_text".equals(goalType) && !"complete_specific_course".equals(goalType);
                course.setVisibility("complete_specific_course".equals(goalType) ? View.VISIBLE : View.GONE);
                number.setVisibility(needsNumber ? View.VISIBLE : View.GONE);
                number.setHint("spend_minutes".equals(goalType) ? "Minutes"
                        : "complete_n_courses".equals(goalType) ? "Number of courses" : "Number of days");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New learning goal" : "Edit goal")
                .setView(form)
                .setPositiveButton(existing == null ? "Add" : "Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        // The button is set up after the dialog shows, so a mistake keeps the form open
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            String goalType = GOAL_TYPES[type.getSelectedItemPosition()];
            if (t.isEmpty()) {
                Toast.makeText(this, "Enter a goal title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Session.isLoggedIn()) {
                Toast.makeText(this, "Log in to add goals", Toast.LENGTH_SHORT).show();
                return;
            }

            // A specific course has to be a real course in the catalogue
            Integer courseId = null;
            if ("complete_specific_course".equals(goalType)) {
                String courseName = course.getText().toString().trim();
                courseId = findCourseId(courseName);
                if (courseId == null) {
                    Toast.makeText(this, courseName.isEmpty() ? "Enter the course name"
                            : "No course called " + courseName, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Courses, minutes and days need a number more than 0
            Integer targetValue = null;
            if (number.getVisibility() == View.VISIBLE) {
                try {
                    targetValue = Integer.parseInt(number.getText().toString().trim());
                } catch (NumberFormatException e) {
                    targetValue = 0;
                }
                if (targetValue <= 0) {
                    Toast.makeText(this, "Enter a target number more than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // The target date is optional, e.g. "Dec 15"
            String targetText = target.getText().toString().trim();
            String targetDate = null;
            if (!targetText.isEmpty()) {
                targetDate = toDatabaseDate(targetText);
                if (targetDate == null) {
                    Toast.makeText(this, "Enter the date like Dec 15", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            goalService.saveGoal(existing == null ? null : existing.id, t, goalType, targetValue,
                    courseId, targetDate, new ApiCallback<Void>() {
                        @Override public void onSuccess(Void result) {
                            dialog.dismiss();
                            // A missed goal with a new date gets another chance
                            if (existing != null && existing.missed) {
                                goalService.reopenGoal(existing.id, new ApiCallback<Void>() {
                                    @Override public void onSuccess(Void r) { loadGoals(); }
                                    @Override public void onError(String error) { loadGoals(); }
                                });
                            } else {
                                loadGoals();
                            }
                        }
                        @Override public void onError(String error) {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }));
        dialog.show();
    }

    // What the goal is, for the types the app checks by itself. Empty for the others.
    private String goalSummary(Goal g) {
        if (g.targetValue == null || g.goalType == null) return "";
        switch (g.goalType) {
            case "complete_n_courses": return "Finish " + g.targetValue + (g.targetValue == 1 ? " course" : " courses");
            case "spend_minutes": return "Spend " + g.targetValue + " minutes learning";
            case "active_days": return "Be active " + g.targetValue + " days in a row";
            default: return "";
        }
    }

    // Logs progress on a goal. A check-in on a new day also keeps the streak going,
    // and the goal may complete straight away if it's now reached.
    private void showCheckInDialog(Goal g) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        EditText note = field("What did you do? (optional)");
        EditText amount = field("spend_minutes".equals(g.goalType) ? "Minutes studied (optional)" : "Amount (optional)");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(note);
        form.addView(amount);

        new AlertDialog.Builder(this)
                .setTitle("Check in: " + g.title)
                .setView(form)
                .setPositiveButton("Log check-in", (d, w) -> {
                    String noteText = note.getText().toString().trim();
                    Integer value = null;
                    try {
                        value = Integer.parseInt(amount.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        // left empty, that's fine
                    }
                    goalService.logCheckIn(g.id, noteText.isEmpty() ? null : noteText, value, new ApiCallback<List<String>>() {
                        @Override public void onSuccess(List<String> unlocked) {
                            Toast.makeText(MainActivity.this, "Check-in logged", Toast.LENGTH_SHORT).show();
                            EasterEggs.celebrate(MainActivity.this, unlocked);
                            loadGoals();
                            loadMe();
                        }
                        @Override public void onError(String error) {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Shows every check-in logged on a goal, newest first.
    private void showCheckIns(Goal g) {
        goalService.getCheckIns(g.id, new ApiCallback<List<CheckIn>>() {
            @Override public void onSuccess(List<CheckIn> result) {
                if (result.isEmpty()) {
                    message("Check-ins", "No check-ins on this goal yet.");
                    return;
                }
                String[] rows = new String[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    CheckIn checkIn = result.get(i);
                    rows[i] = toDisplayDate(checkIn.loggedAt)
                            + (checkIn.note != null ? "  -  " + checkIn.note : "")
                            + (checkIn.valueLogged != null ? "  (" + checkIn.valueLogged + ")" : "");
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Check-ins: " + g.title)
                        .setItems(rows, null)
                        .setPositiveButton("OK", null)
                        .show();
            }
            @Override public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGoalOptions(Goal g) {


        // The options depend on the goal. Only personal goals can be ticked off by hand,
        // because the app completes the other types itself. Missed goals can be reopened.
        ArrayList<String> options = new ArrayList<>();
        if (!g.completed && !g.missed) options.add("Log check-in");
        options.add("View check-ins");
        options.add("Edit goal");
        if (g.completed || g.missed) {
            options.add("Mark as active");
        } else {
            if ("free_text".equals(g.goalType)) options.add("Mark as completed");
            options.add("Behind".equals(g.status) ? "Mark as on track" : "Mark as behind");
        }
        options.add("Delete goal");
        final String[] items = options.toArray(new String[0]);

        new AlertDialog.Builder(this).setTitle(g.title).setItems(items, (d, which) -> {
            String choice = items[which];


            if ("Log check-in".equals(choice)) { showCheckInDialog(g); return; }
            if ("View check-ins".equals(choice)) { showCheckIns(g); return; }
            if ("Edit goal".equals(choice)) { showGoalForm(g); return; }

            ApiCallback<Void> reload = new ApiCallback<Void>() {
                @Override public void onSuccess(Void result) { loadGoals(); }
                @Override public void onError(String error) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            };
            if ("Mark as active".equals(choice)) { goalService.reopenGoal(g.id, reload); return; }
            else if ("Mark as completed".equals(choice)) { goalService.completeGoal(g.id, reload); return; }
            // On track and behind are only shown on this phone, they are not saved
            else if ("Mark as on track".equals(choice)) g.status = "On Track";
            else if ("Mark as behind".equals(choice)) g.status = "Behind";
            else { goalService.deleteGoal(g.id, reload); return; }
            g.lastAccessed = today();
            showGoals();
        }).show();
    }

//LeaderBoard
    private void showLeaderboard() {
        Page p = createPage("Leaderboard", "Compete with other learners and climb the ranks", false);

// leaderboard rankings

        int myRank = 0;
        for (int i = 0; i < leaderboard.size(); i++) if (leaderboard.get(i).you) myRank = i + 1;
        LinearLayout me = new LinearLayout(this);
        me.setOrientation(LinearLayout.HORIZONTAL);
        me.setGravity(Gravity.CENTER_VERTICAL);
        me.setPadding(dp(16), dp(14), dp(16), dp(14));
        me.setBackground(rounded(WHITE, 18));
        TextView rankBadge = text(myRank > 0 ? "#" + myRank : "#-", WHITE, 14, true);
        rankBadge.setGravity(Gravity.CENTER);
        rankBadge.setBackground(oval(PURPLE));
        me.addView(rankBadge, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout meInfo = new LinearLayout(this);
        meInfo.setOrientation(LinearLayout.VERTICAL);
        meInfo.addView(text(userName.isEmpty() ? "Your ranking" : userName, DEEP_NAVY, 16, true));
        meInfo.addView(text(String.format(Locale.getDefault(), "%,d XP", "All Time".equals(selectedPeriod) ? totalXp : userXp), MUTED, 13, false));
        LinearLayout.LayoutParams meInfoLp = new LinearLayout.LayoutParams(0, -2, 1f);
        meInfoLp.leftMargin = dp(12);
        me.addView(meInfo, meInfoLp);

        int streakColor = Color.rgb(176, 38, 197);
        LinearLayout streak = new LinearLayout(this);
        streak.setOrientation(LinearLayout.HORIZONTAL);
        streak.setGravity(Gravity.CENTER_VERTICAL);
        streak.setPadding(dp(10), dp(6), dp(12), dp(6));
        streak.setBackground(rounded(Color.rgb(250, 232, 252), 16));
        streak.addView(iconView("flame", streakColor, 14));
        TextView streakText = text(streakDays + (streakDays == 1 ? " Day" : " Days"), streakColor, 12, true);
        streakText.setPadding(dp(4), 0, 0, 0);
        streak.addView(streakText);
        me.addView(streak);
        LinearLayout.LayoutParams meLp = new LinearLayout.LayoutParams(-1, -2);
        meLp.bottomMargin = dp(14);
        p.content.addView(me, meLp);

        // Weekly,monthly & all time segmented control
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        seg.setBackground(rounded(Color.rgb(226, 231, 241), 26));
        for (String option : new String[]{"Monthly", "All Time"}) {
            boolean sel = selectedPeriod.equals(option);
            TextView t = text(option, sel ? DEEP_NAVY : MUTED, 14, true);
            t.setGravity(Gravity.CENTER);
            if (sel) t.setBackground(rounded(WHITE, 22));
            seg.addView(t, new LinearLayout.LayoutParams(0, dp(40), 1f));
            t.setOnClickListener(v -> { selectedPeriod = option; loadLeaderboard(); showLeaderboard(); });
        }
        LinearLayout.LayoutParams segLp = new LinearLayout.LayoutParams(-1, -2);
        segLp.bottomMargin = dp(14);
        p.content.addView(seg, segLp);

        if (leaderboard.isEmpty()) {
            LinearLayout empty = card(p);
            addText(empty, "No rankings available", DEEP_NAVY, 17, true);
            addText(empty, "Monthly".equals(selectedPeriod) ? "Nobody has earned points this month yet." : "Nobody has earned points yet.", MUTED, 14, false);
        } else {
            for (int i = 0; i < leaderboard.size(); i++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(10);
                p.content.addView(leaderRow(i + 1, leaderboard.get(i)), lp);
            }
        }
        showPage(p.root, false);
    }

    private View leaderRow(int rank, LeaderEntry e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(rounded(e.you ? Color.rgb(232, 236, 255) : WHITE, 14));

        int badgeBg = Color.TRANSPARENT, badgeFg = MUTED;
        if (rank == 1) { badgeBg = Color.rgb(255, 243, 214); badgeFg = Color.rgb(214, 150, 20); }
        else if (rank == 2) { badgeBg = Color.rgb(238, 241, 246); }
        else if (rank == 3) { badgeBg = Color.rgb(255, 236, 222); badgeFg = Color.rgb(217, 119, 48); }
        TextView badge = text(String.valueOf(rank), badgeFg, 13, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(oval(badgeBg));
        row.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(e.name + (e.you ? " (You)" : ""), DEEP_NAVY, 15, true));
        info.addView(text(String.format(Locale.getDefault(), "%,d XP", e.xp), MUTED, 12, false));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
        infoLp.leftMargin = dp(12);
        row.addView(info, infoLp);
        return row;
    }

// user profile tab
    private void showProfile() {
        Page p = createPage("Profile", "Your account and learning profile", false);
        LinearLayout c = card(p);
        addText(c, "Your profile", DEEP_NAVY, 20, true);
        if (Session.isLoggedIn()) {
            User user = Session.getUser();
            addText(c, "Signed in as " + user.fullName, MUTED, 14, false);
            addText(c, user.email, MUTED, 14, false);
            addButton(c, "Log out", true, v -> logout());


            // Name, password, dark mode and text size
            addButton(c, "Settings", false, v -> openScreen(SettingsActivity.class));

            // The learner's points and streak. The leaderboard only counts this month,
            // so the all-time total is shown here.
            LinearLayout numbers = card(p);
            addText(numbers, "Your progress", DEEP_NAVY, 16, true);
            addProfileRow(numbers, "All-time points", String.format(Locale.getDefault(), "%,d XP", totalXp));
            addProfileRow(numbers, "This month", String.format(Locale.getDefault(), "%,d XP", userXp));
            addProfileRow(numbers, "Current streak", streakDays + (streakDays == 1 ? " day" : " days"));
            addProfileRow(numbers, "Longest streak", longestStreak + (longestStreak == 1 ? " day" : " days"));
            if (Boolean.FALSE.equals(linked)) {
                addText(numbers, "Not linked to LearnTech yet, so course points can't be earned.", MUTED, 12, false);
            }
            addButton(numbers, "Achievements and badges", false, v -> openScreen(AchievementsActivity.class));
            addButton(numbers, "Momentum Map", true, v -> openScreen(MomentumMapActivity.class));


            // All-time points on each of the last 14 days with a sync, as a line chart
            LinearLayout chart = card(p);
            addText(chart, "Points over time", DEEP_NAVY, 16, true);
            List<ProgressSnapshot> recent = progress.subList(Math.max(0, progress.size() - 14), progress.size());
            if (recent.size() < 2) {
                addText(chart, "Your chart fills in as you learn. Each day your progress is synced from "
                        + "LearnTech adds a point to it.", MUTED, 13, false);
            } else {
                int[] points = new int[recent.size()];
                for (int i = 0; i < points.length; i++) {
                    points[i] = recent.get(i).totalPoints;
                }
                LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(-1, dp(140));
                chartLp.topMargin = dp(8);
                chart.addView(new PointsChart(this, points), chartLp);

                LinearLayout dates = new LinearLayout(this);
                dates.setOrientation(LinearLayout.HORIZONTAL);
                dates.addView(text(shortDay(recent.get(0).day), MUTED, 11, false),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                TextView last = text(shortDay(recent.get(recent.size() - 1).day), MUTED, 11, false);
                last.setGravity(Gravity.END);
                dates.addView(last, new LinearLayout.LayoutParams(0, -2, 1f));
                chart.addView(dates, new LinearLayout.LayoutParams(-1, -2));

                ProgressSnapshot latest = recent.get(recent.size() - 1);
                addText(chart, latest.coursesCompleted + " courses completed, "
                        + latest.totalMinutes + " minutes learned", MUTED, 12, false);
            }
        } else {
            addText(c, "Sign in or create an account to access your learning profile.", MUTED, 14, false);
            addButton(c, "Log in", false, v -> startActivity(new Intent(this, LoginActivity.class)));
            addButton(c, "Create account", true, v -> startActivity(new Intent(this, SignupActivity.class)));
        }



        // Only administrators see the admin panel. The backend checks the role as well.
        if (Session.isLoggedIn() && Session.getUser().isAdmin()) {
            LinearLayout admin = card(p);
            addText(admin, "Administration", DEEP_NAVY, 16, true);
            addText(admin, "Manage users, courses, badges and the LearnTech connection.", MUTED, 12, false);
            addButton(admin, "Open admin panel", true, v ->
                            startActivity(new Intent(MainActivity.this, AdminPanel.class)));
        }

        showPage(p.root, false);
    }


    // One label and number row on the profile.
    private void addProfileRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(text(label, MUTED, 14, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(value, DEEP_NAVY, 14, true));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    // Login, register and logout

    private void logout() {
        authService.logout();
        loadedUserId = -1;
        userName = "";
        userXp = 0;
        totalXp = 0;
        streakDays = 0;
        longestStreak = 0;
        courses.clear();
        otherCourses.clear();
        linked = null;
        goals.clear();
        leaderboard.clear();
        catalogue.clear();
        progress.clear();
        showStudentTab("Profile");
    }

    // Loading data from the backend

    private void loadData() {
        courseService.getCatalogue(new ApiCallback<List<com.example.momentum.model.Course>>() {
            @Override public void onSuccess(List<com.example.momentum.model.Course> result) {
                catalogue.clear();
                catalogue.addAll(result);
                // Goals and courses show the course's name, so they load after the catalogue
                loadGoals();
                loadCourses();
            }
            @Override public void onError(String error) {
                loadGoals();
            }
        });
        loadLeaderboard();
        loadMe();
    }

    // Gets the latest rankings, points and streak each time the Leaderboard or
    // Profile tab is opened, so they're never out of date.
    private void refreshIfLoggedIn() {
        if (Session.isLoggedIn()) {
            loadLeaderboard();
            loadMe();
        }
    }

    // The learner's own numbers: linked or not, all-time points and the streak
    // from their check-ins, which the backend keeps.
    private void loadMe() {
        profileService.getMe(new ApiCallback<User>() {
            @Override public void onSuccess(User me) {
                linked = me.isLinked();
                userName = me.fullName;
                totalXp = me.totalPoints != null ? me.totalPoints : 0;
                streakDays = me.currentStreak != null ? me.currentStreak : 0;
                longestStreak = me.longestStreak != null ? me.longestStreak : 0;
                if ("main".equals(currentScreen)) {
                    showStudentTab(currentTab);
                }


                // Keeps the saved active days up to date, for the streak reminder
                profileService.getActiveDays(new ApiCallback<List<String>>() {
                    @Override public void onSuccess(List<String> days) { }
                    @Override public void onError(String error) { }
                });

                // Points over time, for the chart on the Profile tab
                profileService.getProgress(new ApiCallback<List<ProgressSnapshot>>() {
                    @Override public void onSuccess(List<ProgressSnapshot> result) {
                        progress.clear();
                        progress.addAll(result);
                        if ("main".equals(currentScreen) && "Profile".equals(currentTab)) {
                            showStudentTab(currentTab);
                        }
                    }
                    @Override public void onError(String error) { }
                });
            }
            @Override public void onError(String error) {
                // Leaves the linked note out


                // The saved login ran out: back to logged out, with the reason
                if (!Session.isLoggedIn() && loadedUserId != -1) {
                    logout();
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadCourses() {
        courseService.getMyCourses(new ApiCallback<List<Enrolment>>() {
            @Override public void onSuccess(List<Enrolment> result) {
                courses.clear();
                for (Enrolment enrolment : result) {
                    com.example.momentum.model.Course info = catalogueCourse(enrolment.courseId);
                    Course c = courseFrom(info);
                    c.status = "completed".equals(enrolment.state) ? "Completed"
                            : "in_progress".equals(enrolment.state) ? "In Progress" : "New";
                    c.timeSpentMinutes = enrolment.timeSpentMinutes;
                    c.completedOn = enrolment.completedOn == null ? "" : enrolment.completedOn;

                    // A completed course shows what it really earned. 0 means it didn't
                    // pass the time check (at least 70% of the expected time spent in it).
                    if ("Completed".equals(c.status)) {
                        c.xp = enrolment.pointsEarned != null ? enrolment.pointsEarned : 0;
                        c.xpText = c.xp + " XP";
                    }
                    courses.add(c);
                }
                fillOtherCourses();
                if ("main".equals(currentScreen) && ("Home".equals(currentTab) || "Courses".equals(currentTab))) {
                    showStudentTab(currentTab);
                }
            }
            @Override public void onError(String error) {
                // The Courses tab still shows the rest of the catalogue
                fillOtherCourses();
            }
        });
    }

    // Every catalogue course the learner isn't enrolled on.
    private void fillOtherCourses() {
        otherCourses.clear();
        for (com.example.momentum.model.Course info : catalogue) {
            boolean enrolled = false;
            for (Course c : courses) {
                if (c.courseId == info.courseId) {
                    enrolled = true;
                    break;
                }
            }
            if (!enrolled) {
                Course c = courseFrom(info);
                c.notEnrolled = true;
                otherCourses.add(c);
            }
        }
    }

    // A course for the screens, from the catalogue. The points shown are what it's
    // worth when completed: the admin's value, otherwise its expected minutes.
    // Fast Course is the demo course, worth a random 1 to 30.
    private Course courseFrom(com.example.momentum.model.Course info) {
        Course c = new Course();
        if (info == null) {
            c.title = "Course";
            return c;
        }
        c.courseId = info.courseId;
        c.title = info.name;
        c.expectedMinutes = info.expectedMinutes;
        if (info.platformCourseId == FAST_COURSE_ID) {
            c.xpText = "1-30 XP";
        } else {
            c.xp = info.pointsValue != null ? info.pointsValue
                    : info.expectedMinutes != null ? info.expectedMinutes : 10;
            c.xpText = c.xp + " XP";
        }
        return c;
    }

    private com.example.momentum.model.Course catalogueCourse(int courseId) {
        for (com.example.momentum.model.Course c : catalogue) {
            if (c.courseId == courseId) return c;
        }
        return null;
    }

    private void loadGoals() {
        goalService.getGoals(new ApiCallback<List<com.example.momentum.model.Goal>>() {
            @Override public void onSuccess(List<com.example.momentum.model.Goal> result) {
                goals.clear();
                for (com.example.momentum.model.Goal saved : result) {
                    if ("cancelled".equals(saved.status)) continue;
                    Goal g = new Goal(saved.id, saved.title);
                    g.course = courseName(saved.targetCourseId);
                    g.target = toDisplayDate(saved.targetDate);
                    g.lastAccessed = toDisplayDate(saved.createdAt);
                    g.completed = "completed".equals(saved.status);
                    g.completedDate = toDisplayDate(saved.completedAt);
                    g.goalType = saved.goalType;
                    g.targetValue = saved.targetValue;
                    g.targetCourseId = saved.targetCourseId;
                    g.targetDate = saved.targetDate;
                    g.missed = "failed".equals(saved.status);
                    if (g.missed) g.status = "Missed";
                    goals.add(g);
                }
                if ("Goals".equals(currentTab) && "main".equals(currentScreen)) showGoals();
            }
            @Override public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLeaderboard() {
        boolean allTime = "All Time".equals(selectedPeriod);
        leaderboardService.getLeaderboard(allTime, new ApiCallback<List<LeaderboardEntry>>() {
            @Override public void onSuccess(List<LeaderboardEntry> result) {
                // The learner switched period before this answer arrived: it's out of date
                if (allTime != "All Time".equals(selectedPeriod)) return;
                leaderboard.clear();
                for (LeaderboardEntry entry : result) {
                    LeaderEntry e = new LeaderEntry();
                    e.name = entry.name;
                    e.xp = entry.points;
                    e.you = Session.getUser() != null && entry.userId == Session.getUser().id;
                    if (e.you && !allTime) userXp = entry.points;
                    leaderboard.add(e);
                }
                // The rankings arrive after the screen is drawn, so draw it again
                if ("main".equals(currentScreen) && "Leaderboard".equals(currentTab)) {
                    showLeaderboard();
                }
            }
            @Override public void onError(String error) {
                // No leaderboard yet, the screen shows its empty message
            }
        });
    }

    // Finds a course in the catalogue by its name, ignoring capitals. Null if there is none.
    private Integer findCourseId(String name) {
        for (com.example.momentum.model.Course c : catalogue) {
            if (c.name.equalsIgnoreCase(name)) return c.courseId;
        }
        return null;
    }

    private String courseName(Integer courseId) {
        if (courseId == null) return "";
        for (com.example.momentum.model.Course c : catalogue) {
            if (c.courseId == courseId) return c.name;
        }
        return "";
    }

    // "Dec 15" becomes "2026-12-15" for the database. Null if it isn't a date.
    private String toDatabaseDate(String text) {
        try {
            SimpleDateFormat typed = new SimpleDateFormat("MMM d yyyy", Locale.ENGLISH);
            typed.setLenient(false);
            String year = new SimpleDateFormat("yyyy", Locale.ENGLISH).format(new Date());
            Date date = typed.parse(text + " " + year);

            // A date that has already passed this year means next year, so "Jan 5"
            // typed in December doesn't make a goal that's already missed.
            java.util.Calendar startOfToday = java.util.Calendar.getInstance();
            startOfToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
            startOfToday.set(java.util.Calendar.MINUTE, 0);
            startOfToday.set(java.util.Calendar.SECOND, 0);
            startOfToday.set(java.util.Calendar.MILLISECOND, 0);
            if (date.before(startOfToday.getTime())) {
                java.util.Calendar nextYear = java.util.Calendar.getInstance();
                nextYear.setTime(date);
                nextYear.add(java.util.Calendar.YEAR, 1);
                date = nextYear.getTime();
            }
            return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    // "2026-12-15" or "2026-12-15 10:30:00" from the database becomes "Dec 15".
    private String toDisplayDate(String value) {
        if (value == null || value.length() < 10) return "";
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(value.substring(0, 10));
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "";
        }
    }

  //Course details
    private void showCourseDetails(Course c) {
        currentScreen = "courseDetails";
        checkTab(TAB_COURSES);

        Page p = createPage(null, null, false);
        p.content.setPadding(dp(20), p.content.getPaddingTop(), dp(20), dp(100));
        addBack(p.content, "Back to Courses", v -> showStudentTab("Courses"));

        View hero = new View(this);
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(14, 78, 100), DEEP_NAVY});
        heroBg.setCornerRadius(dp(16));
        hero.setBackground(heroBg);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, dp(140));
        heroLp.bottomMargin = dp(14);
        p.content.addView(hero, heroLp);

        TextView title = text(c == null ? "Course details" : c.title, WHITE, 24, true);
        p.content.addView(title, new LinearLayout.LayoutParams(-1, -2));

        // The course's details from LearnTech: status, time, points and dates
        p.content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(16)));
        if (c != null) {
            addCourseDetails(p.content, c);
        }

        // Pinned gradient button
        TextView cta = text("Continue Learning", WHITE, 16, true);
        cta.setGravity(Gravity.CENTER);
        GradientDrawable ctaBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{PURPLE, TEAL});
        ctaBg.setCornerRadius(dp(28));
        cta.setBackground(ctaBg);
        FrameLayout.LayoutParams ctaLp = new FrameLayout.LayoutParams(-1, dp(54), Gravity.BOTTOM);
        ctaLp.setMargins(dp(20), 0, dp(20), dp(20));
        p.root.addView(cta, ctaLp);
        cta.setOnClickListener(v -> {
            // The course itself is on the learning platform, so open it inside the app.
            // Opening it doesn't count towards the streak, time spent learning in it does.
            Intent learnTech = new Intent(MainActivity.this, LearnTechActivity.class);
            learnTech.putExtra(LearnTechActivity.EXTRA_URL, LMS_URL);
            startActivity(learnTech);
        });

        // A completed course has a certificate
        if (c != null && "Completed".equals(c.status)) {
            addButton(p.content, "View certificate", true, v -> {
                Intent intent = new Intent(MainActivity.this, CertificateActivity.class);
                intent.putExtra("courseTitle", c.title);
                intent.putExtra("completedOn", c.completedOn);
                startActivity(intent);
            });
        }

        showPage(p.root, false);
    }


    // The course's details from LearnTech, on the course page.
    private void addCourseDetails(LinearLayout parent, Course c) {
        addHeading(parent, "Course Details");

        String state = c.notEnrolled ? "Not enrolled"
                : "New".equals(c.status) ? "Not started"
                : c.status;
        addDetailRow(parent, "Status", state);
        addDetailRow(parent, "Expected time",
                c.expectedMinutes == null ? "Not set" : c.expectedMinutes + " minutes");
        if (!c.notEnrolled) {
            addDetailRow(parent, "Time spent", c.timeSpentMinutes + " minutes");
        }
        addDetailRow(parent, "Completed".equals(c.status) ? "Points earned" : "Points", c.xpText);
        if (!c.completedOn.isEmpty()) {
            addDetailRow(parent, "Completed on", toDisplayDate(c.completedOn));
        }
    }

    // One label and value row on the course page.
    private void addDetailRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(rounded(Color.argb(45, 255, 255, 255), 14));
        row.addView(text(label, Color.argb(225, 255, 255, 255), 14, false),
                new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(value, WHITE, 14, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        parent.addView(row, lp);
    }


    // Progress ring (Home)

    private static class RingView extends View {
        private final float progress;
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);

        RingView(Context context, float progress) {
            super(context);
            this.progress = progress;
            float density = context.getResources().getDisplayMetrics().density;
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(10 * density);
            track.setColor(TRACK);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeWidth(10 * density);
            arc.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            float half = track.getStrokeWidth() / 2f;
            RectF r = new RectF(half, half, w - half, h - half);
            canvas.drawArc(r, 0, 360, false, track);
            if (progress > 0f) {
                arc.setShader(new SweepGradient(w / 2f, h / 2f, new int[]{TEAL, PURPLE, TEAL}, null));
                canvas.save();
                canvas.rotate(-90, w / 2f, h / 2f);
                canvas.drawArc(r, 0, 360f * progress, false, arc);
                canvas.restore();
            }
        }
    }

    // "2026-10-22" as "22 Oct", for under the chart.
    private String shortDay(String day) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(day);
            return new SimpleDateFormat("d MMM", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return day;
        }
    }

    // A line chart of points, one dot per day, with the area under it shaded.
    // The lowest value sits at the bottom and the highest at the top.
    private static class PointsChart extends View {
        private final int[] values;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;

        PointsChart(Context context, int[] values) {
            super(context);
            this.values = values;
            density = context.getResources().getDisplayMetrics().density;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(3 * density);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setColor(PURPLE);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(Color.argb(40, 118, 102, 232));
            dot.setColor(TEAL);
            label.setColor(MUTED);
            label.setTextSize(11 * density);
        }

        @Override protected void onDraw(Canvas canvas) {
            int min = values[0], max = values[0];
            for (int v : values) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            float pad = 8 * density;
            float top = 18 * density;           // room for the highest value's label
            float w = getWidth() - 2 * pad;
            float h = getHeight() - top - pad;
            float range = Math.max(1, max - min);

            Path path = new Path();
            Path area = new Path();
            float[] xs = new float[values.length];
            float[] ys = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                xs[i] = pad + w * i / (values.length - 1);
                ys[i] = top + h - h * (values[i] - min) / range;
                if (i == 0) {
                    path.moveTo(xs[i], ys[i]);
                    area.moveTo(xs[i], top + h);
                }
                else path.lineTo(xs[i], ys[i]);
                area.lineTo(xs[i], ys[i]);
            }
            area.lineTo(xs[values.length - 1], top + h);
            area.close();

            canvas.drawPath(area, fill);
            canvas.drawPath(path, line);
            for (int i = 0; i < values.length; i++) {
                canvas.drawCircle(xs[i], ys[i], 4 * density, dot);
            }

            // The newest total, above its dot
            String latest = values[values.length - 1] + " XP";
            float textX = Math.min(xs[values.length - 1] - label.measureText(latest) / 2,
                    getWidth() - label.measureText(latest));
            canvas.drawText(latest, Math.max(0, textX), ys[values.length - 1] - 8 * density, label);
        }
    }

    // Outline icons drawn in code

    private static class Icon extends Drawable {
        private final String type;
        private final int fixedColor;
        private int tint = MUTED;
        private int alpha = 255;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        Icon(String type, int fixedColor) {
            this.type = type;
            this.fixedColor = fixedColor;
            if (fixedColor != 0) tint = fixedColor;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2f);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override public void setTintList(ColorStateList list) { /* colours are handled internally */ }
        @Override public boolean isStateful() { return fixedColor == 0; }
        @Override protected boolean onStateChange(int[] state) {
            if (fixedColor != 0) return false;
            int c = MUTED;
            for (int s : state) {
                if (s == android.R.attr.state_checked || s == android.R.attr.state_selected) c = PURPLE;
            }
            boolean changed = c != tint;
            tint = c;
            if (changed) invalidateSelf();
            return changed;
        }

        private Path poly(float... xy) {
            Path path = new Path();
            path.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
            path.close();
            return path;
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.scale(b.width() / 24f, b.height() / 24f);
            stroke.setColor(tint);
            fill.setColor(tint);
            stroke.setAlpha(alpha);
            fill.setAlpha(alpha);
            Path p = new Path();
            switch (type) {
                case "home":
                    p.moveTo(3, 11); p.lineTo(12, 3); p.lineTo(21, 11);
                    canvas.drawPath(p, stroke);
                    p.reset(); p.moveTo(5, 9.5f); p.lineTo(5, 21); p.lineTo(19, 21); p.lineTo(19, 9.5f);
                    canvas.drawPath(p, stroke);
                    p.reset(); p.moveTo(10, 21); p.lineTo(10, 14); p.lineTo(14, 14); p.lineTo(14, 21);
                    canvas.drawPath(p, stroke);
                    break;
                case "book":
                    canvas.drawRoundRect(5, 3, 19, 21, 2.5f, 2.5f, stroke);
                    canvas.drawLine(5, 17, 19, 17, stroke);
                    canvas.drawLine(9, 7, 15, 7, stroke);
                    break;
                case "target":
                    canvas.drawCircle(12, 12, 9, stroke);
                    canvas.drawCircle(12, 12, 5.5f, stroke);
                    canvas.drawCircle(12, 12, 2f, fill);
                    break;
                case "trophy":
                    p.moveTo(7, 4); p.lineTo(17, 4); p.lineTo(17, 10);
                    p.quadTo(17, 15, 12, 15); p.quadTo(7, 15, 7, 10); p.close();
                    canvas.drawPath(p, stroke);
                    p.reset(); p.moveTo(7, 6); p.lineTo(4, 6); p.quadTo(4, 11, 7, 11);
                    canvas.drawPath(p, stroke);
                    p.reset(); p.moveTo(17, 6); p.lineTo(20, 6); p.quadTo(20, 11, 17, 11);
                    canvas.drawPath(p, stroke);
                    canvas.drawLine(12, 15, 12, 19, stroke);
                    canvas.drawLine(8, 20.5f, 16, 20.5f, stroke);
                    break;
                case "person":
                    canvas.drawCircle(12, 8, 4, stroke);
                    p.moveTo(4, 21); p.quadTo(4, 14, 12, 14); p.quadTo(20, 14, 20, 21);
                    canvas.drawPath(p, stroke);
                    break;
                case "hex":
                    canvas.drawPath(poly(12, 2, 21, 7, 21, 17, 12, 22, 3, 17, 3, 7), stroke);
                    canvas.drawPath(poly(12, 7, 16.5f, 9.5f, 16.5f, 14.5f, 12, 17, 7.5f, 14.5f, 7.5f, 9.5f), stroke);
                    break;
                case "check":
                    p.moveTo(6, 12.5f); p.lineTo(10.5f, 17); p.lineTo(18, 8);
                    canvas.drawPath(p, stroke);
                    break;
                case "search":
                    canvas.drawCircle(10.5f, 10.5f, 6.5f, stroke);
                    canvas.drawLine(15.5f, 15.5f, 21, 21, stroke);
                    break;
                case "plus":
                    canvas.drawLine(12, 5, 12, 19, stroke);
                    canvas.drawLine(5, 12, 19, 12, stroke);
                    break;
                case "star":
                    canvas.drawPath(poly(12, 2, 14.9f, 8.5f, 22, 9.2f, 16.8f, 14, 18.2f, 21,
                            12, 17.4f, 5.8f, 21, 7.2f, 14, 2, 9.2f, 9.1f, 8.5f), fill);
                    break;
                case "flame":
                    p.moveTo(12, 3);
                    p.cubicTo(12, 8, 17, 10, 17, 15);
                    p.cubicTo(17, 18.5f, 14.8f, 21, 12, 21);
                    p.cubicTo(9.2f, 21, 7, 18.5f, 7, 15);
                    p.cubicTo(7, 12.5f, 8.5f, 11, 9.5f, 9.5f);
                    p.cubicTo(10.5f, 11, 11, 11.5f, 12, 12);
                    p.cubicTo(12.5f, 9, 12, 6, 12, 3);
                    p.close();
                    canvas.drawPath(p, fill);
                    break;
                case "up":
                    canvas.drawLine(12, 19, 12, 6, stroke);
                    p.moveTo(6, 12); p.lineTo(12, 6); p.lineTo(18, 12);
                    canvas.drawPath(p, stroke);
                    break;
                case "down":
                    canvas.drawLine(12, 5, 12, 18, stroke);
                    p.moveTo(6, 12); p.lineTo(12, 18); p.lineTo(18, 12);
                    canvas.drawPath(p, stroke);
                    break;
                case "dash":
                    canvas.drawLine(7, 12, 17, 12, stroke);
                    break;
            }
            canvas.restore();
        }

        @Override public void setAlpha(int a) { alpha = a; invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) {
            stroke.setColorFilter(filter);
            fill.setColorFilter(filter);
            invalidateSelf();
        }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}