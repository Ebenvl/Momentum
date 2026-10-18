package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.StudySession;

import java.util.List;

// The learner's weekly study timetable.
public class StudyService {

    private final MomentumApi api = Api.get();

    public void getSessions(ApiCallback<List<StudySession>> callback) {
        api.getStudySessions(callback);
    }

    // A null id adds a new session, otherwise that session is updated.
    public void saveSession(Integer id, String day, String time, String course, String task,
                            ApiCallback<Void> callback) {
        api.saveStudySession(id, day, time, course, task, callback);
    }

    public void deleteSession(int id, ApiCallback<Void> callback) {
        api.deleteStudySession(id, callback);
    }
}
