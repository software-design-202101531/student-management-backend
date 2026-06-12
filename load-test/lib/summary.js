// 공통 결과 리포트 헬퍼.
// 각 시나리오가 끝나면 handleSummary() 가 호출되어:
//   1) HTML 리포트를 ${K6_REPORT_DIR}/${K6_REPORT}.html 로 저장 (CI 아티팩트로 업로드)
//   2) 콘솔에는 기존과 동일한 텍스트 요약을 출력
//   3) 기계 판독용 raw JSON 도 함께 저장
//
// 원격 라이브러리는 버전 고정(핀)으로 재현성 확보:
//   - htmlReport: benc-uk/k6-reporter 2.4.0
//   - textSummary: jslib k6-summary 0.0.2
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/2.4.0/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

export function handleSummary(data) {
  const dir = __ENV.K6_REPORT_DIR || 'k6-report';
  const name = __ENV.K6_REPORT || 'summary';
  return {
    [`${dir}/${name}.html`]: htmlReport(data, { title: name }),
    [`${dir}/${name}.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
