package com.momentum.backend.sync;

import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.EnrolmentRepository;
import com.momentum.backend.db.SnapshotRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.learntech.ApiCourse;
import com.momentum.backend.learntech.ApiEnrolment;
import com.momentum.backend.learntech.ApiLearner;
import com.momentum.backend.learntech.CoursesResponse;
import com.momentum.backend.learntech.LearnTechClient;
import com.momentum.backend.learntech.LearnTechException;
import com.momentum.backend.learntech.LearnersResponse;
import com.momentum.backend.model.Course;
import com.momentum.backend.model.User;

import java.sql.SQLException;

// Copies data from LearnTech into our own database.
//
// The app never waits on LearnTech. The backend syncs on a schedule, and the
// app only ever reads what is already in our database.
public class SyncService {

    private final LearnTechClient learnTechClient;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final SnapshotRepository snapshotRepository;

    public SyncService(LearnTechClient learnTechClient, CourseRepository courseRepository,
                       UserRepository userRepository, EnrolmentRepository enrolmentRepository,
                       SnapshotRepository snapshotRepository) {
        this.learnTechClient = learnTechClient;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.snapshotRepository = snapshotRepository;
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

    // Goes through every learner on LearnTech. If one of our app users has the
    // same email, they get linked and their courses are saved.
    // Learners who never registered in the app are skipped.
    // Gives back how many learners were linked to an app account.
    public int syncLearners() throws LearnTechException, SQLException {

        LearnersResponse response = learnTechClient.getAllLearners();
        int linked = 0;

        for (ApiLearner learner : response.learners) {

            User user = userRepository.findUserByEmail(learner.email);
            if (user == null) {
                continue;   // no app account with this email, skip to the next learner
            }

            // Link the account the first time we see a match.
            if (user.idst == null) {
                if (userRepository.idstTaken(learner.idst)) {
                    continue;   // this learner is already linked to a different account
                }
                userRepository.updateIdst(user.userId, learner.idst);
            }

            for (ApiEnrolment enrolment : learner.courses) {

                // Our own id for this course. If the course isn't in our catalogue yet, add it first.
                Course course = courseRepository.findCourseByPlatformId(enrolment.courseId);
                if (course == null) {
                    courseRepository.saveCourse(enrolment.courseId, enrolment.courseName, enrolment.expectedMinutes);
                    course = courseRepository.findCourseByPlatformId(enrolment.courseId);
                }

                // The assessment can be null, so only read inside it if it's there.
                String assessmentStatus = null;
                Double score = null;
                Double scoreMax = null;
                if (enrolment.assessment != null) {
                    assessmentStatus = enrolment.assessment.status;
                    score = enrolment.assessment.score;
                    scoreMax = enrolment.assessment.scoreMax;
                }

                enrolmentRepository.saveEnrolment(user.userId, course.courseId, enrolment.state,
                        enrolment.timeSpentMinutes, assessmentStatus, score, scoreMax,
                        enrolment.firstAccess, enrolment.dateCompleted);
            }

            // Records where the learner stands right now. LearnTech keeps no
            // history, so this is the only history there will be.
            // Points and streak are 0 until those parts are built.
            snapshotRepository.insertSnapshot(user.userId,
                    learner.summary.coursesEnrolled,
                    learner.summary.coursesCompleted,
                    learner.summary.totalTimeMinutes,
                    0,
                    0);

            linked++;
        }

        return linked;
    }
}
