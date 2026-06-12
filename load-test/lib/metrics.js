import { Rate, Trend } from 'k6/metrics';

// 409 = 낙관적 락(@Version) 충돌. 정상적인 비즈니스 결과이므로 "실패"가 아니라 관찰 지표로 둔다.
export const conflictRate = new Rate('grade_conflicts');

// 200/201/204/409 외 응답만 진짜 실패로 집계한다. 쓰기 시나리오의 합격 기준.
export const businessErrors = new Rate('business_errors');

// OLAP 반영 지연(end-to-end ETL lag, ms). 쓰기 → analytics 반영까지의 시간.
export const etlLag = new Trend('etl_lag', true);

// 쓰기 응답 1건을 위 메트릭에 기록한다.
export function recordWrite(res) {
  conflictRate.add(res.status === 409);
  businessErrors.add(![200, 201, 204, 409].includes(res.status));
}
