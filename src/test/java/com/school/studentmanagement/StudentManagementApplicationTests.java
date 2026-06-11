package com.school.studentmanagement;

import com.school.studentmanagement.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

// 전체 컨텍스트 기동 스모크. 공유 Postgres(Testcontainers) 베이스를 상속해 외부 DB 없이도 동작한다.
class StudentManagementApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {
    }

}
