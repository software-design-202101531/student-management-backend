# 상담 내역(Consultation) 관리 기능 구현 보고서

> 작성일: 2026-05-25
> 대상 브랜치: `main`
> 관련 모듈: `com.school.studentmanagement.consultation`

---

## 1. 개요

교사가 학생과의 상담 내역을 기록·관리하고, **공개 범위(visibility)와 요청자의 권한(role·담임 여부·작성자 여부)을 조합한 OR 조건**으로 열람을 통제하는 기능을 구현했다.

상담은 학생/학부모에게 노출되는 피드백과 달리 **교사·관리자 내부용 기록**이다. 따라서 조회 자체가 교사/관리자에게만 허용되며, 그 안에서도 다음 네 가지 OR 조건 중 하나 이상을 만족해야 개별 상담을 볼 수 있다.

> ① 관리자(ROLE_ADMIN) · ② `visibility = ALL_TEACHERS` · ③ 작성자 본인 · ④ 해당 학생의 담임 교사

---

## 2. 데이터베이스(DB) 엔티티 설계 변경점

### 2.1 `Student` 테이블 — 담임 교사 FK 추가

권한 체크(④ 담임 교사 여부)를 위해 학생이 자신의 담임을 직접 가리킬 수 있어야 한다. `Student` 엔티티에 담임 교사 FK를 추가했다. — `student/entity/Student.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `homeroom_teacher_id` | BIGINT | FK(`teachers`), **NULL 허용** | 담임 교사 (미배정 시 null) |

- `@ManyToOne(fetch = LAZY)`로 `Teacher`를 참조한다.
- 담임 배정/판별을 위한 도메인 메서드를 추가했다.
  - `assignHomeroomTeacher(Teacher)` — 담임 배정/변경
  - `isHomeroomTeacher(Long teacherId)` — null-safe 담임 여부 판별
- **설계 비고**: 기존 코드베이스에는 학기 단위로 담임을 표현하는 `Classroom.homeroomTeacher`(반 → 담임) 구조가 이미 존재한다. 본 기능은 명세 요구에 따라 권한 검증을 단순·고속화하기 위해 `Student`에 담임을 **직접(비정규화)** 두었다. 초기 데이터(`InitDataConfig`)에서 학급 담임과 동일하게 채워 두 모델의 정합성을 유지한다. (학기별 담임 변경까지 추적해야 한다면 추후 `StudentAffiliation` 기반 조회로 대체 가능)

### 2.2 `consultation` 테이블 (신규)

`Consultation` 엔티티 (`consultations` 테이블) — `consultation/entity/Consultation.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, Auto Increment | 식별자 |
| `teacher_id` | BIGINT | FK(`teachers`), NOT NULL | 상담을 기록한 작성자 교사 |
| `student_id` | BIGINT | FK(`students`), NOT NULL | 대상 학생 |
| `consultation_date` | TIMESTAMP | NOT NULL | 상담 일시 |
| `content` | TEXT(`@Lob`) | NOT NULL | 주요 내용 |
| `next_plan` | TEXT(`@Lob`) | NULL 허용 | 다음 상담 계획 |
| `visibility` | VARCHAR(20) | NOT NULL, ENUM(String) | 공개 범위 (기본값 `RESTRICTED`) |
| `created_at` | TIMESTAMP | NOT NULL, 수정 불가 | 생성 시각 |
| `updated_at` | TIMESTAMP | NOT NULL | 최종 수정 시각 |

- enum은 기존 컨벤션을 따라 `@Enumerated(EnumType.STRING)`으로 저장한다.
- 상태 변경은 도메인 메서드(`create`, `toggleVisibility`)로만 수행한다.
- 생성 시 `visibility`가 null이면 엔티티 팩토리(`Consultation.create`)에서 `RESTRICTED`로 강제한다.

### 도메인 Enum — `global/enums/ConsultationVisibility.java`

| 값 | 설명 |
|----|------|
| `RESTRICTED` | 제한적 공개 (작성자/담임/관리자만, **기본값**) |
| `ALL_TEACHERS` | 전체 교사 공개 |

`toggle()` 메서드로 `RESTRICTED ↔ ALL_TEACHERS` 전환을 캡슐화했다.

---

## 3. 권한 검증(Authorization) 비즈니스 로직

`ConsultationService` 서비스 레이어에서 방어막을 친다. (프로젝트 컨벤션상 `@PreAuthorize` 미사용 → **경로 기반 인가 + 서비스 단 검증** 병행)

### 3.1 조회 권한 통과 조건 (OR)

`getStudentConsultations`에서 학생당 담임 여부(`isHomeroom`)를 **1회만** 평가한 뒤, 각 상담 행에 대해 아래 조건 중 하나라도 만족하면 응답에 포함한다. 전부 불만족이면 해당 행은 제외(목록에서 필터링)된다.

```java
private boolean canView(Consultation c, Long requesterId, boolean isAdmin, boolean isHomeroom) {
    return isAdmin                                              // ① 관리자
            || c.getVisibility() == ConsultationVisibility.ALL_TEACHERS  // ② 전체 교사 공개
            || c.isAuthor(requesterId)                          // ③ 작성자 본인
            || isHomeroom;                                      // ④ 해당 학생의 담임
}
```

> 목록 조회는 "해당 학생의 전체 상담을 DB에서 조회 → 권한 조건으로 메모리 필터링" 방식을 택했다. (명세에서 허용한 두 방식 중 메모리 필터링)

### 3.2 공개 범위 변경(토글) 권한

`toggleVisibility`는 **작성자 본인 또는 관리자**만 호출 가능하다. 둘 다 아니면 `403 Forbidden`(`ACCESS_DENIED`).

---

## 4. REST API 엔드포인트

> 명세의 `/api/v1/...`에서 `v1`을 제외하고, 기존 코드베이스 컨벤션(`/api/...`)에 맞춰 통일했다.

### 4.1 `POST /api/consultations` — 상담 내역 생성

- **접근 권한**: `ROLE_TEACHER` 전용
- **로직**: `visibility` 미지정 시 `RESTRICTED`로 저장. 작성자(`teacher_id`)는 토큰의 사용자로 자동 설정.
- **요청 본문**
  ```json
  {
    "studentId": 101,
    "consultationDate": "2026-05-25T14:30:00",
    "content": "진로 및 학습 태도 상담 진행. 목표 학과에 대한 동기 부여.",
    "nextPlan": "2주 후 모의고사 결과 기반 재상담",
    "visibility": "RESTRICTED"
  }
  ```

### 4.2 `GET /api/students/{studentId}/consultations` — 특정 학생 상담 조회

- **접근 권한**: `ROLE_TEACHER` / `ROLE_ADMIN`
- **로직**: 해당 학생의 전체 상담을 조회한 뒤 §3.1 OR 조건으로 필터링하여 응답. 상담 일시(`consultation_date`) 내림차순 정렬.

### 4.3 `PATCH /api/consultations/{consultationId}/visibility` — 공개 범위 변경(토글)

- **접근 권한**: 작성자 본인 또는 `ROLE_ADMIN`
- **로직**: `RESTRICTED ↔ ALL_TEACHERS` 토글. 별도 요청 본문 없이 현재 상태를 반전시키며, 변경된 상담 정보를 응답으로 반환해 클라이언트가 즉시 반영할 수 있게 했다.

---

## 5. 접근 제어(RBAC) 적용 방식

1. **경로 기반(1차 방어선)** — `SecurityConfig`
   ```java
   .requestMatchers(HttpMethod.POST,  "/api/consultations").hasRole("TEACHER")
   .requestMatchers(HttpMethod.GET,   "/api/students/*/consultations").hasAnyRole("TEACHER", "ADMIN")
   .requestMatchers(HttpMethod.PATCH, "/api/consultations/*/visibility").hasAnyRole("TEACHER", "ADMIN")
   ```
   - 학생/학부모 토큰은 조회 단계에서 차단된다(상담은 교사·관리자 내부 기록).
   - 작성(POST)을 `TEACHER`로 한정 → 관리자에게는 `Teacher` 레코드가 없어 작성 주체가 될 수 없으므로 자연스럽게 교사 전용.

2. **서비스 계층(2차 방어선)** — 경로로 거를 수 없는 세부 권한
   - 조회: §3.1 OR 조건 행 단위 필터링
   - 토글: 작성자 본인 또는 관리자 검증

인가 실패 시 기존 `BusinessException` → `GlobalExceptionHandler` 흐름으로 일관된 `ApiResponse` 에러 포맷을 응답한다.

---

## 6. 추가/변경 파일 목록

### 신규 파일

```
src/main/java/com/school/studentmanagement/
├── global/enums/ConsultationVisibility.java     # 공개 범위 enum (+toggle)
└── consultation/
    ├── entity/Consultation.java                 # 엔티티 + 도메인 메서드
    ├── repository/ConsultationRepository.java    # 학생별 전체 조회(fetch join)
    ├── dto/
    │   ├── ConsultationCreateRequest.java
    │   └── ConsultationResponse.java
    ├── service/ConsultationService.java          # 생성/조회(OR필터)/토글
    └── controller/
        ├── ConsultationController.java           # POST / PATCH(visibility)
        └── StudentConsultationController.java     # GET (목록)
```

### 수정 파일

- `student/entity/Student.java` — `homeroomTeacher` FK + `assignHomeroomTeacher`/`isHomeroomTeacher` 추가
- `global/config/InitDataConfig.java` — 학생 시딩 시 학급 담임과 동일하게 `homeroomTeacher` 채움 (2반→박국어, 4반→최수학, 5반→이영어 / 3반 홍길동은 담임 미배정 null)
- `global/exception/ErrorCode.java` — `CONSULTATION_NOT_FOUND` 추가
- `global/security/SecurityConfig.java` — 상담 엔드포인트 경로 규칙 추가

---

## 7. 추가 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `CONSULTATION_NOT_FOUND` | 404 | 상담 내역을 찾을 수 없습니다 |
| (재사용) `ACCESS_DENIED` | 403 | 공개 범위 변경 권한 불일치 시 |
| (재사용) `STUDENT_NOT_FOUND` | 404 | 대상 학생 부재 |
| (재사용) `TEACHER_NOT_FOUND` | 404 | 작성자 교사 부재 |

---

## 8. 검증

- `./gradlew compileJava`, `./gradlew compileTestJava` 모두 컴파일 성공.
- DB 스키마는 `application.yml`의 `ddl-auto: create`로 기동 시 `consultations` 테이블 및 `students.homeroom_teacher_id` 컬럼이 자동 생성된다.
- 초기 데이터로 권한 시나리오 검증이 가능하다.
  - 4반 학생 담임: `teacher03`(최수학) / 5반 학생 담임: `teacher04`(이영어) / 2반 학생 담임: `teacher02`(박국어)
  - 예) `teacher01`(김수학)이 5반 학생의 `RESTRICTED` 상담을 조회하면, 작성자도 담임도 아니므로 목록에서 제외됨. 해당 상담을 `ALL_TEACHERS`로 토글하면 비로소 조회 가능.

---

## 9. 향후 확장 포인트

- **담임 모델 일원화**: `Student.homeroomTeacher`(직접 참조)와 `Classroom.homeroomTeacher`(학기 단위) 중 하나로 정책 확정. 학기별 담임 이력이 중요해지면 `StudentAffiliation` 기반 조회로 대체.
- **공개 범위 세분화**: 현재 2단계(RESTRICTED/ALL_TEACHERS)에서 학년/교과 단위 공개 등으로 확장 가능.
- **쿼리단 필터링 전환**: 상담 데이터가 많아지면 메모리 필터링을 동적 쿼리(QueryDSL 등)로 옮겨 성능 최적화.
- **상담 수정/삭제 API**: 현재는 생성·조회·공개범위 변경만 제공. 본문 수정/삭제 엔드포인트 추가 가능.
