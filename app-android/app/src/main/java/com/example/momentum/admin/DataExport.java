package com.example.momentum.admin;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.momentum.data.ApiCallback;
import com.example.momentum.service.AdminService;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// Data export: the admin saves any of the main tables as a CSV file, which
// opens in Excel, for backup or reporting. The phone asks where to save it.
public class DataExport extends AdminScreen {

    // The tables the backend can export, and how they're shown.
    private static final String[] TABLES = {"users", "courses", "enrolments", "goals", "checkins",
            "points", "badges", "user_badges", "streaks", "snapshots", "logs"};
    private static final String[] TABLE_NAMES = {"Users", "Courses", "Enrolments", "Goals", "Check-ins",
            "Points", "Badges", "Earned badges", "Streaks", "Progress snapshots", "Activity log"};

    private final AdminService adminService = new AdminService();
    private ActivityResultLauncher<String> saveFile;
    private String pendingTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Has to be set up before the screen starts. It opens the phone's
        // "save as" screen, and gives back where the learner chose to save.
        saveFile = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
            if (uri != null && pendingTable != null) {
                download(pendingTable, uri);
            }
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String title() {
        return "Export Data";
    }

    @Override
    protected String subtitle() {
        return "Save any table as a CSV file for backup or reporting";
    }

    @Override
    protected void load() {
        content.addView(text("Pick a table, then where to save it. The file opens in Excel. "
                + "Users leaves out passwords.", 13, MUTED, false));
        for (int i = 0; i < TABLES.length; i++) {
            String table = TABLES[i];
            button(TABLE_NAMES[i], false, v -> {
                pendingTable = table;
                saveFile.launch("momentum-" + table + ".csv");
            });
        }
    }

    // Gets the table from the backend and writes it to the file the admin picked.
    private void download(String table, Uri uri) {
        toast("Exporting...");
        adminService.exportTable(table, new ApiCallback<String>() {
            @Override
            public void onSuccess(String csv) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    out.write(csv.getBytes(StandardCharsets.UTF_8));
                    toast("Saved");
                } catch (Exception e) {
                    toast("Could not save the file");
                }
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }
}
