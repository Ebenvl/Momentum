package com.example.momentum.placeholder;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.Course;
import com.example.momentum.service.CourseService;

import java.util.ArrayList;
import java.util.List;

// TEMPORARY courses screen.
public class CoursesActivity extends BaseActivity {

    private final CourseService courseService = new CourseService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireLogin()) return;
        setContentView(R.layout.temp_list);
        applyInsets(findViewById(R.id.root));

        ((TextView) findViewById(R.id.titleText)).setText("My courses");
        TextView infoText = findViewById(R.id.infoText);
        ListView list = findViewById(R.id.list);

        courseService.getCourses(Session.getUser().id, new ApiCallback<List<Course>>() {
            @Override
            public void onSuccess(List<Course> courses) {
                int completed = 0;
                List<String> rows = new ArrayList<>();
                for (Course c : courses) {
                    if ("completed".equals(c.state)) completed++;
                    rows.add(c.name + "\n" + c.stateLabel() + "  ·  " + c.timeSpentMinutes + " min spent");
                }
                infoText.setText(completed + " of " + courses.size() + " courses complete");
                list.setAdapter(new ArrayAdapter<>(CoursesActivity.this,
                        android.R.layout.simple_list_item_1, rows));
            }

            @Override
            public void onError(String message) {
                infoText.setText(message);
            }
        });
    }
}
