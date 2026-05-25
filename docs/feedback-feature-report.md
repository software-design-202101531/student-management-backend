# 피드백(Feedback) 기능 구현 보고서

> 작성일: 2026-05-25
> 대상 브랜치: `main`
> 관련 모듈: `com.school.studentmanagement.feedback`

---

## 1. 개요

교사가 학생에 대한 피드백을 작성·관리하고, 권한에 따라 학생·학부모가 이를 열람할 수 있는 기능을 구현했다.

설계의 핵심은 **데이터의 생명주기(작성 상태, `status`)와 노출 권한(공개 여부, `isPublic`)을 독립된 두 필드로 분리**한 점이다. 이를 통해 다음과 같은 실무 시나리오를 모두 표현할 수 있다.

| status | isPublic | 의미 |
|--------|----------|------|
| DRAFT | false | 교사가 작성 중인 임시 메모 (교사 전용) |
| DRAFT | true | 공개 예정이나 아직 발행 전 (교사 간 공유) |
| PUBLISHED | false | 발행되었으나 교사 내부 기록용 (학생/학부모 비공개) |
| PUBLISHED | true | 발행 + 공개 → **학생/학부모 열람 가능** |

즉, 학생/학부모에게 노출되는 조건은 `status = PUBLISHED AND isPublic = true` 두 가지를 **동시에** 만족하는 경우로 한정된다.

---

## 2. 데이터베이스(DB) 엔티티 구조

`Feedback` 엔티티 (`feedbacks` 테이블) — `feedback/entity/Feedback.java`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, Auto Increment | 데이터 식별자 |
| `teacher_id` | BIGINT | FK(`teachers`), NOT NULL | 작성자(교사) |
| `student_id` | BIGINT | FK(`students`), NOT NULL | 대상 학생 |
| `category` | VARCHAR(20) | NOT NULL, ENUM(String) | 분류 |
| `content` | TEXT(`@Lob`) | NOT NULL | 피드백 본문 (글자 수 제한 없음) |
| `status` | VARCHAR(20) | NOT NULL, ENUM(String) | 작성 상태(생명주기) |
| `is_public` | BOOLEAN | NOT NULL | 공개 여부(노출 권한) |
| `created_at` | TIMESTAMP | NOT NULL, 수정 불가 | 생성 시각 |
| `updated_at` | TIMESTAMP | NOT NULL | 최종 수정 시각 |

- 연관관계는 프로젝트 컨벤션에 맞춰 모두 `@ManyToOne(fetch = LAZY)`로 설정했다.
- `teacher` / `student`는 각각 `Teacher`, `Student` 엔티티를 참조하며, 두 엔티티는 `@MapsId` 구조라 PK가 곧 `User.id`와 동일하다. (→ JWT의 `userId`로 곧바로 교사/학생을 식별 가능)
- enum은 기존 엔티티(`User`, `Exam` 등) 컨벤션을 따라 `@Enumerated(EnumType.STRING)`으로 문자열 저장한다.
- 상태 변경은 setter 없이 도메인 메서드(`create`, `update`, `publish`)로만 수행하여 불변식을 보장한다.

### 도메인 Enum

`global/enums/FeedbackCategory.java`

| 값 | 설명 |
|----|------|
| `GRADE` | 성적 |
| `BEHAVIOR` | 행동 |
| `ATTENDANCE` | 출결 |
| `ATTITUDE` | 태도 |
| `ETC` | 기타 |

`global/enums/FeedbackStatus.java`

| 값 | 설명 |
|----|------|
| `DRAFT` | 임시저장 |
| `PUBLISHED` | 발행 완료 |

---

## 3. REST API 엔드포인트 및 접근 제어(RBAC)

> 엔드포인트 경로는 기존 코드베이스의 다른 도메인과 일관되도록 `/api/...` prefix로 통일했다. (명세의 `/api/v1/...`에서 `v1`을 제외)

### 3.1 `POST /api/feedbacks` — 피드백 생성

- **접근 권한**: `ROLE_TEACHER` 전용
- **로직**: 최초 작성 시 `status = DRAFT`로 고정 저장 (엔티티 생성자에서 강제). `isPublic` 미지정 시 `false`.
- **요청 본문**
  ```json
  {
    "studentId": 101,
    "category": "ATTITUDE",
    "content": "수업 태도가 매우 성실합니다.",
    "isPublic": true
  }
  ```
- **응답**: 생성된 피드백 정보(`FeedbackResponse`)

### 3.2 `GET /api/students/{studentId}/feedbacks` — 피드백 목록 조회

- **접근 권한**: 교사 / 학생 / 학부모 (인증 토큰 필수)
- **권한별 조회 필터링 분기** (`FeedbackService.getStudentFeedbacks`)

  | 권한 | 조회 범위 | 추가 검증 |
  |------|-----------|-----------|
  | **교사** | 해당 학생의 **모든** 피드백 (status/isPublic 무관) | 없음 — 타 교사의 임시저장·비공개 기록도 공유 |
  | **학생** | `status = PUBLISHED AND isPublic = true` 인 건만 | `studentId == 본인 userId` 검증, 불일치 시 403 |
  | **학부모** | `status = PUBLISHED AND isPublic = true` 인 건만 | `ParentStudentMapping`으로 연결된 자녀인지 검증, 불일치 시 403 |

- 조회는 `createdAt DESC` 정렬, 교사 이름 표시를 위해 `teacher`·`user`를 `JOIN FETCH`로 함께 로딩한다.

### 3.3 `PUT /api/feedbacks/{feedbackId}` — 피드백 수정

- **접근 권한**: 해당 피드백을 작성한 **교사 본인만** (`teacher_id` 일치 검증, 불일치 시 403)
- **로직**: 본문(`content`)·분류(`category`)·공개옵션(`isPublic`)을 수정. `updated_at` 갱신.

### 3.4 `PATCH /api/feedbacks/{feedbackId}/publish` — 피드백 최종 발행

- **접근 권한**: 작성한 **교사 본인만**
- **로직**: `status` 값을 `DRAFT` → `PUBLISHED`로 변경. 이미 발행된 건이면 `409 FEEDBACK_ALREADY_PUBLISHED`.
- **비고**: 현재는 상태 변경만 수행한다. 향후 알림 시스템 도입 시 `FeedbackService.publishFeedback` 내부 발행 직후 지점에서 비동기 알림 이벤트(`ApplicationEventPublisher` 등)를 트리거하도록 확장 가능하도록 주석으로 자리를 명시해 두었다.

---

## 4. RBAC 구현 방식

본 프로젝트는 `@PreAuthorize`(메서드 시큐리티)를 사용하지 않고, **경로 기반 인가 + 서비스 계층 검증**을 병행하는 방식을 따른다. 이 컨벤션을 그대로 적용했다.

1. **경로 기반(1차 방어선)** — `SecurityConfig`에 교사 전용 쓰기 엔드포인트 규칙 추가
   ```java
   .requestMatchers(HttpMethod.POST,  "/api/feedbacks").hasRole("TEACHER")
   .requestMatchers(HttpMethod.PUT,   "/api/feedbacks/*").hasRole("TEACHER")
   .requestMatchers(HttpMethod.PATCH, "/api/feedbacks/*/publish").hasRole("TEACHER")
   ```
   목록 조회 `GET`은 별도 역할 규칙 없이 `.anyRequest().authenticated()`에 위임되어 인증된 모든 권한이 접근 가능하다.

2. **서비스 계층(2차 방어선)** — 경로 규칙으로 거를 수 없는 세부 권한 검증
   - 조회 시 요청자 `role`에 따른 데이터 필터링 분기
   - 학생: 본인 학생 ID 일치 검증
   - 학부모: 자녀 연결(매핑) 검증
   - 수정/발행: 작성자 본인 여부 검증 (`Feedback.isAuthor`)

인가 실패 시 기존 `BusinessException` + `GlobalExceptionHandler` 흐름을 타고 일관된 `ApiResponse` 에러 포맷으로 응답한다.

---

## 5. 추가/변경 파일 목록

### 신규 파일

```
src/main/java/com/school/studentmanagement/
├── global/enums/
│   ├── FeedbackCategory.java          # 분류 enum
│   └── FeedbackStatus.java            # 상태 enum
└── feedback/
    ├── entity/Feedback.java           # 엔티티 + 도메인 메서드
    ├── repository/FeedbackRepository.java  # 권한별 조회 쿼리
    ├── dto/
    │   ├── FeedbackCreateRequest.java
    │   ├── FeedbackUpdateRequest.java
    │   └── FeedbackResponse.java
    ├── service/FeedbackService.java   # 생성/조회/수정/발행 비즈니스 로직
    └── controller/
        ├── FeedbackController.java          # POST / PUT / PATCH
        └── StudentFeedbackController.java   # GET (목록)
```

### 수정 파일

- `global/exception/ErrorCode.java` — `FEEDBACK_NOT_FOUND`, `FEEDBACK_ALREADY_PUBLISHED` 추가
- `global/security/SecurityConfig.java` — 교사 전용 쓰기 엔드포인트 경로 규칙 추가

---

## 6. 추가 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `FEEDBACK_NOT_FOUND` | 404 | 피드백 정보를 찾을 수 없습니다 |
| `FEEDBACK_ALREADY_PUBLISHED` | 409 | 이미 발행된 피드백입니다 |
| (재사용) `ACCESS_DENIED` | 403 | 작성자/권한 불일치 시 상세 메시지와 함께 반환 |
| (재사용) `STUDENT_NOT_FOUND` | 404 | 생성 시 대상 학생 부재 |
| (재사용) `TEACHER_NOT_FOUND` | 404 | 작성자 교사 부재 |

---

## 7. 검증

- `./gradlew compileJava` 컴파일 성공 (신규/수정 소스 정상 빌드 확인).
- DB 스키마는 `application.yml`의 `ddl-auto: create` 설정으로 애플리케이션 기동 시 `feedbacks` 테이블이 자동 생성된다. (별도 마이그레이션 불필요)

---

## 8. 향후 확장 포인트

- **알림 연동**: `publishFeedback` 발행 시점에서 학생/학부모 대상 비동기 알림 이벤트 발행.
- **페이징/검색**: 목록 조회에 `Pageable`, 카테고리·기간 필터 추가.
- **작성 권한 세분화**: 현재는 "교사 권한"이면 누구나 작성 가능. 추후 담임/담당 과목 교사 등으로 작성 권한을 제한할 수 있다 (기존 `record` 모듈의 `validateHomeroomTeacherAuthority` 패턴 참고).
