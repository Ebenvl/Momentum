package com.example.momentum.placeholder;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.User;
import com.example.momentum.service.AdminService;

import java.util.ArrayList;
import java.util.List;

// TEMPORARY admin screen. Tap a user to suspend or reactivate them.
public class AdminActivity extends BaseActivity {

    private final AdminService adminService = new AdminService();
    private final List<User> users = new ArrayList<>();
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireLogin()) return;
        setContentView(R.layout.temp_list);
        applyInsets(findViewById(R.id.root));

        ((TextView) findViewById(R.id.titleText)).setText("Admin: users");
        ((TextView) findViewById(R.id.infoText)).setText("Tap a user to suspend or reactivate");
        list = findViewById(R.id.list);

        list.setOnItemClickListener((parent, view, position, id) ->
                adminService.toggleSuspended(users.get(position), new ApiCallback<User>() {
                    @Override
                    public void onSuccess(User updated) {
                        toast(updated.fullName + " is now " + updated.status);
                        loadUsers();
                    }

                    @Override
                    public void onError(String message) {
                        toast(message);
                    }
                }));

        loadUsers();
    }

    private void loadUsers() {
        adminService.getAllUsers(new ApiCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                users.clear();
                users.addAll(result);
                List<String> rows = new ArrayList<>();
                for (User u : users) {
                    rows.add(u.fullName + "  (" + u.role + ")\n" + u.email + "  ·  " + u.status);
                }
                list.setAdapter(new ArrayAdapter<>(AdminActivity.this,
                        android.R.layout.simple_list_item_1, rows));
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }
}
