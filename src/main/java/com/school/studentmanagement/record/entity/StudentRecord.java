package com.school.studentmanagement.record.entity;

import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "student_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_record", // DB에 저장될 제약조건 이름
                        columnNames = {
                                "student_id",       // 누구의
                                "academic_year",    // 몇 년도
                                "semester",         // 몇 학기에
                                "record_category",  // 과세특이고
                                "subject_id"        // 무슨 과목인지
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기록의 대상(학생)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // 기록 작성 교사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // 학년도
    @Column(nullable = false)
    private Integer academicYear;

    // 학기
    @Column(nullable = false)
    private Integer semester;

    // 세특인지 행특인지 구분
    @Column(nullable = false)
    private RecordCategory recordCategory;

    // 세특은 필수, 행특은 Null(이로 인해 행특은 중복 위험 있음 처리할 것)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="subject_id")
    private Subject subject;

    @Lob // 대용량 텍스트 처리
    @Column(nullable = false)
    private String content;

    // 생성자
    private StudentRecord(Student student, Teacher teacher, Integer academicYear, Integer semester, RecordCategory recordCategory, Subject subject, String content) {
        this.student = student;
        this.teacher = teacher;
        this.academicYear = academicYear;
        this.semester = semester;
        this.recordCategory = recordCategory;
        this.subject = subject;
        this.content = content;
    }

    // 세특 생성 메서드
    public static StudentRecord createSubjectOpinion(Student student, Teacher teacher, Integer academicYear, Integer semester, Subject subject, String content) {
        // 만일 과목이 없다면 예외 던지기
        if (subject == null) {
            throw new IllegalArgumentException("반드시 과목 정보가 필요합니다");
        }

        return new StudentRecord(
                student, teacher, academicYear, semester,
                RecordCategory.SUBJECT_OPINION,
                subject, content
        );
    }

    // 행특 전용 생성 메서드
    public static StudentRecord createBehaviorOpinion(Student student, Teacher teacher, Integer academicYear, Integer semester, String content) {
        return new StudentRecord(
                student, teacher, academicYear, semester,
                RecordCategory.BEHAVIOR_OPINION,
                null, content
        );
    }

    // 행특 내용 수정 메서드
    public void updateContent(String newContent, Teacher teacher) {
        this.content = newContent;
        // 담임 선생님이 바뀐 경우를 가정
        this.teacher = teacher;
    }
}
