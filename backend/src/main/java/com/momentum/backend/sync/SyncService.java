package com.momentum.backend.sync;

import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.learntech.ApiCourse;
import com.momentum.backend.learntech.CoursesResponse;
import com.momentum.backend.learntech.LearnTechClient;
import com.momentum.backend.learntech.LearnTechException;

import java.sql.SQLException;

// Copies data from LearnTech into our own database.
// This is the Sync Service from D3, section 6.2.
//
// The app never waits on LearnTech. The backend syncs on a schedule, and the
// app only ever reads what is already in our database (NFR-17).
public class SyncService {

    private final LearnTechClient learnTechClient;
    private final CourseRepository courseRepository;

    public SyncService(LearnTechClient learnTechClient, CourseRepository courseRepository) {
        this.learnTechClient = learnTechClient;
        this.courseRepository = courseRepository;
    }

    // Saves LearnTech's whole course catalogue into the Courses table.
    // New courses are added, existing ones are updated (saveCourse does both).
    // Gives back how many courses were saved.
    public int syncCourses() throws LearnTechException, SQLException {

        CoursesResponse response = learnTechClient.getCourses();
        int saved = 0;

        for (ApiCourse course : response.courses) {
            courseRepository.saveCourse(course.courseId, course.courseName, course.expectedMinutes);
            saved++;
        }

        return saved;
    }
}
