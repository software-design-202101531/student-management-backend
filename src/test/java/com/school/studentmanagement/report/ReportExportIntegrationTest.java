package com.school.studentmanagement.report;

import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.security.JwtTokenProvider;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.support.IntegrationTestSupport;
import com.school.studentmanagement.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보고서 export end-to-end 통합 테스트.
 * 실제 Postgres(Testcontainers) + 전체 Spring 컨텍스트(Security/JWT 필터/JPA/POI/PDF)를 거친다.
 * 클래스 레벨 @Transactional 로 테스트 데이터는 매 테스트 후 롤백된다(컨트롤러 트랜잭션이 같은 스레드 tx에 합류).
 */
@AutoConfigureMockMvc
@Transactional
class ReportExportIntegrationTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String teacherToken;
    private String studentToken;
    private Long studentId;

    @BeforeEach
    void setUp() {
        User teacher = User.builder()
                .loginId("teacher-" + System.nanoTime()).password("x").name("김교사")
                .gender(Gender.MALE).role(UserRole.TEACHER).status(UserStatus.ACTIVE).build();
        em.persist(teacher);

        User studentUser = User.builder()
                .loginId("student-" + System.nanoTime()).password("x").name("학생")
                .gender(Gender.MALE).role(UserRole.STUDENT).status(UserStatus.ACTIVE).build();
        em.persist(studentUser);

        // @MapsId — 프로덕션과 동일하게 id 미지정으로 persist (id는 user에서 파생)
        Student student = Student.builder().user(studentUser).enrollmentYear(2026).build();
        em.persist(student);
        em.flush(); // IDENTITY id 확정 + 같은 tx 내 조회(JWT 필터의 findById) 가시성 보장

        teacherToken = jwtTokenProvider.createAccessToken(teacher.getId(), "TEACHER");
        studentId = studentUser.getId();
        studentToken = jwtTokenProvider.createAccessToken(studentUser.getId(), "STUDENT");
    }

    @Test
    @DisplayName("토큰 없으면 401")
    void noToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/exports/students/1/feedbacks.xlsx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 권한은 export 접근 403")
    void studentRole_forbidden() throws Exception {
        mockMvc.perform(get("/api/exports/students/1/feedbacks.xlsx")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("교사 피드백 엑셀 다운로드 — 200 + xlsx(zip) 매직바이트 + 첨부 헤더")
    void teacherFeedbacksXlsx() throws Exception {
        byte[] body = mockMvc.perform(get("/api/exports/students/1/feedbacks.xlsx")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body.length).isGreaterThan(0);
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'K'); // xlsx = zip
    }

    @Test
    @DisplayName("교사 피드백 PDF 다운로드 — 200 + %PDF 매직바이트 (한글 폰트 임베딩 포함)")
    void teacherFeedbacksPdf() throws Exception {
        byte[] body = mockMvc.perform(get("/api/exports/students/1/feedbacks.pdf")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(body, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("교사 상담 엑셀 다운로드 — 실존 학생 대상 200")
    void teacherConsultationsXlsx() throws Exception {
        mockMvc.perform(get("/api/exports/students/" + studentId + "/consultations.xlsx")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml")));
    }
}
