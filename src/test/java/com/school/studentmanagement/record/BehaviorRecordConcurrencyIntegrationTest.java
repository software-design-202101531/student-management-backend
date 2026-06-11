package com.school.studentmanagement.record;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.global.enums.EmploymentStatus;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.record.dto.BehaviorRecordRequest;
import com.school.studentmanagement.record.service.StudentRecordService;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.support.IntegrationTestSupport;
import com.school.studentmanagement.teacher.entity.Teacher;
import com.school.studentmanagement.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행특 동시 입력의 정합성 통합 테스트 (기획 §5.1).
 * 여러 교사(스레드)가 같은 학생 행특을 동시에 저장해도:
 *  - 중복 행이 생기지 않고(부분 유니크 인덱스),
 *  - 갱신 손실이 조용히 발생하지 않으며(@Version → 409),
 *  - rollback-only 함정(UnexpectedRollbackException)이 터지지 않는다(ON CONFLICT DO NOTHING 수렴).
 */
class BehaviorRecordConcurrencyIntegrationTest extends IntegrationTestSupport {

    @Autowired private StudentRecordService studentRecordService;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AcademicCalendarUtil academicCalendarUtil;

    private Long teacherId;
    private Long studentId;
    private int year;
    private int semester;

    @BeforeEach
    void setUp() {
        year = academicCalendarUtil.getCurrentAcademicYear();
        semester = academicCalendarUtil.getCurrentSemester();
        long nano = System.nanoTime();

        // 커밋된 기반 데이터(담임=teacher, 학생이 그 반에 소속)를 트랜잭션으로 저장 → 동시 스레드가 볼 수 있게.
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Subject subject = new Subject("과목-" + nano);
            em.persist(subject);

            User teacherUser = User.builder().loginId("t-" + nano).password("x").name("담임교사")
                    .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
            em.persist(teacherUser);
            Teacher teacher = Teacher.builder().user(teacherUser).employeeNumber("EMP-" + nano)
                    .subject(subject).officeLocation("본관").officePhoneNumber("02-000")
                    .employmentStatus(EmploymentStatus.ACTIVE).build();
            em.persist(teacher);

            User studentUser = User.builder().loginId("s-" + nano).password("x").name("학생")
                    .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
            em.persist(studentUser);
            Student student = Student.builder().user(studentUser).enrollmentYear(year).build();
            em.persist(student);

            // 시드 데이터(1학년 1~5반)와 (year,semester,grade,class_num) 유니크 충돌을 피하도록 고유 classNum 사용
            int classNum = 1000 + (int) Math.floorMod(nano, 100_000);
            Classroom classroom = Classroom.builder().academicYear(year).semester(semester)
                    .grade(9).classNum(classNum).homeroomTeacher(teacher).build();
            em.persist(classroom);
            em.persist(StudentAffiliation.builder().student(student).classroom(classroom).studentNum(1).build());
            em.flush();

            teacherId = teacherUser.getId();
            studentId = studentUser.getId();
        });
    }

    @Test
    @DisplayName("여러 교사 동시 행특 저장 — 행은 정확히 1개, rollback-only/예기치 못한 오류 없음")
    void concurrentBehaviorSave_keepsSingleRow_noLostUpdateCrash() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final String content = "교사" + i + "-내용";
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    studentRecordService.saveBehaviorRecord(studentId, teacherId, request(content));
                    return "OK";
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException
                         | org.springframework.dao.DataIntegrityViolationException e) {
                    return "CONFLICT"; // 정상 경합(409로 매핑됨)
                } catch (org.springframework.transaction.UnexpectedRollbackException e) {
                    return "ROLLBACK_ONLY"; // 발생하면 안 됨
                } catch (Exception e) {
                    return "ERR:" + e.getClass().getSimpleName();
                }
            }));
        }

        ready.await();
        start.countDown(); // 동시 출발

        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get());
        }
        pool.shutdown();

        // 1) rollback-only 함정 미발생, 예기치 못한 오류 없음
        assertThat(results).noneMatch(r -> r.equals("ROLLBACK_ONLY") || r.startsWith("ERR:"));
        // 2) 최소 한 명은 성공
        assertThat(results).contains("OK");
        // 3) 결과는 OK 또는 정상 경합(CONFLICT)뿐
        assertThat(results).allMatch(r -> r.equals("OK") || r.equals("CONFLICT"));

        // 4) 핵심: 행특 row가 정확히 1개 (중복 insert 차단)
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM student_records " +
                        "WHERE student_id = ? AND record_category = 'BEHAVIOR_OPINION' " +
                        "AND academic_year = ? AND semester = ? AND subject_id IS NULL",
                Integer.class, studentId, year, semester);
        assertThat(rowCount).isEqualTo(1);
    }

    // BehaviorRecordRequest는 공개 생성자/세터가 없어 단위 테스트와 동일하게 리플렉션으로 구성
    private BehaviorRecordRequest request(String content) {
        try {
            Constructor<BehaviorRecordRequest> ctor =
                    ReflectionUtils.accessibleConstructor(BehaviorRecordRequest.class);
            BehaviorRecordRequest req = ctor.newInstance();
            ReflectionTestUtils.setField(req, "content", content);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
