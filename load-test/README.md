# k6 부하 테스트

학생 성적·상담 관리 시스템의 성능 요구사항 검증용 k6 부하 테스트.
설계 배경과 시나리오 정의는 부하 테스트 계획서를 참고.

## 사전 준비

1. **k6 설치** — `brew install k6` (macOS) 또는 [공식 설치 가이드](https://grafana.com/docs/k6/latest/set-up/install-k6/)
2. **앱 스택 기동** (데모 시드 포함)
   ```bash
   docker compose up -d
   # 앱은 dev 프로필 + app.demo.enabled=true 로 기동되어 시드 계정이 생성됨
   ```
3. **헬스 체크** — `curl localhost:8080/actuator/health` (없으면 로그인으로 대체)

## 실행

```bash
# 단일 시나리오 — 부하 패턴은 TEST_TYPE 으로 선택
TEST_TYPE=smoke  k6 run scenarios/s1_grade_write_concurrency.js
TEST_TYPE=stress k6 run scenarios/s1_grade_write_concurrency.js

# 전 시나리오 일괄
TEST_TYPE=smoke ./run-all.sh
./run-all.sh s1_grade_write_concurrency s2_read_spike   # 특정만

# 원격 대상 지정
BASE_URL=https://staging.example.com TEST_TYPE=load k6 run scenarios/s2_read_spike.js
```

### TEST_TYPE (부하 패턴)

| 값 | 목적 |
|----|------|
| `smoke` (기본) | 스크립트·기본 동작 검증 (VU 1, 1분) |
| `load` | 정상 업무 시간대 기준선 (50 VU) |
| `stress` | 한계점 탐색 (50→450 VU 점증) |
| `spike` | 순간 급증 (5→300 VU) |
| `soak` | 장시간 안정성 (40 VU, ~1시간) |

## 시나리오

| 파일 | 계획서 | 대상 | 비고 |
|------|--------|------|------|
| `s0_login.js` | 인증 | `POST /api/auth/login` | 토큰은 Authorization 헤더로 옴 |
| `s1_grade_write_concurrency.js` | 3.1 🔴 | `PUT .../grades/{id}` | 소수 gradeId 핫스팟 → @Version 락 경합 |
| `s2_read_spike.js` | 3.2 🔴 | 성적 목록 + 학생 overview | DB 직격 → HikariCP 포화 관찰 |
| `s3_search_filter.js` | 3.3 🟡 | `GET .../grades/search` | **seed-bulk.sql 적재 후** 의미 있음 |
| `s4_notification.js` | 3.4 🟡 | unread-count 폴링 + 목록 | 큐는 RabbitMQ UI 병행 |
| `s5_olap_dashboard.js` | 3.5 🟡 | analytics overview | `ETL_LAG_PROBE=1` 로 반영 지연 측정 |

## ⚠️ 핵심 설계: 409 는 실패가 아니다

성적 쓰기는 낙관적 락(`@Version`)이라 동시 수정 시 **409(RECORD_CONFLICT)** 가 정상 발생한다.
k6 기본값은 status≥400 을 `http_req_failed` 로 집계하므로, 쓰기 시나리오는 이를 합격 기준으로 쓰지 않는다.

- `grade_conflicts` (Rate) — 409 충돌률, **관찰용**
- `business_errors` (Rate) — 200/409 외 응답만 = 진짜 실패, **합격 기준** (`rate<0.01`)

(`lib/metrics.js`, `config/thresholds.js` 참고)

## 환경변수

| 변수 | 기본 | 설명 |
|------|------|------|
| `BASE_URL` | `http://localhost:8080` | 대상 |
| `TEST_TYPE` | `smoke` | 부하 패턴 |
| `CLASSROOM_ID` / `SUBJECT_ID` | `1` / `1` | 대상 반/과목 (데모: 1반/국어) |
| `ACADEMIC_YEAR` / `SEMESTER` | `2026` / `1` | 대상 학기 |
| `HOT_GRADE_COUNT` | `5` | 동시성 핫스팟 gradeId 개수 (작을수록 경합↑) |
| `TEACHER_PW` / `STUDENT_PW` / `ADMIN_PW` | `test1234!` / `test1234!` / `admin1234!` | 시드 비밀번호 |
| `ETL_LAG_PROBE` | (off) | s5 에서 ETL 반영 지연 측정 활성화 |

## 함께 볼 서버 지표

k6 는 클라이언트(요청) 측만 측정한다. 병목 원인은 서버에서 본다.

- **Spring**: `/actuator/prometheus` (HikariCP, 스레드, GC) — *노출 설정 필요*
- **PostgreSQL**: `pg_stat_activity`, `pg_locks`, `pg_stat_statements`
- **RabbitMQ**: Management UI `:15672` (큐 depth, consumer lag)
- **Redis**: 캐시 적중률(알림 카운트)

## 결과 출력

```bash
k6 run --out json=result.json scenarios/s2_read_spike.js
# Grafana 대시보드: --out influxdb=... (docker-compose 에 influxdb+grafana 추가 시)
```
