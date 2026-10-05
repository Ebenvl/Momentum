package com.example.momentum.placeholder;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.service.LeaderboardService;

import java.util.ArrayList;
import java.util.List;

// TEMPORARY leaderboard screen. Ranked on this month's points only.
public class LeaderboardActivity extends BaseActivity {

    private final LeaderboardService leaderboardService = new LeaderboardService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireLogin()) return;
        setContentView(R.layout.temp_list);
        applyInsets(findViewById(R.id.root));

        ((TextView) findViewById(R.id.titleText)).setText("Leaderboard");
        TextView infoText = findViewById(R.id.infoText);
        infoText.setText("Points earned this month");
        ListView list = findViewById(R.id.list);

        leaderboardService.getLeaderboard(new ApiCallback<List<LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<LeaderboardEntry> entries) {
                List<String> rows = new ArrayList<>();
                for (LeaderboardEntry e : entries) {
                    rows.add("#" + e.rank + "   " + e.name + "   " + e.points + " pts");
                }
                list.setAdapter(new ArrayAdapter<>(LeaderboardActivity.this,
                        android.R.layout.simple_list_item_1, rows));
            }

            @Override
            public void onError(String message) {
                infoText.setText(message);
            }
        });
    }
}
