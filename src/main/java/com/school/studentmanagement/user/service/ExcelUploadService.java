package com.school.studentmanagement.user.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.invitation.entity.ParentInvitation;
import com.school.studentmanagement.user.dto.ExcelStudent;
import com.school.studentmanagement.user.entity.Student;
import com.school.studentmanagement.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelUploadService {

    private final EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public void uploadStudentExcel(MultipartFile file) {
        // 파일 검증
        if ( file == null || file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("올바른 엑셀 파일(.xlsx)을 업로드 해 주세요");
        }

        List<ExcelStudent> dtoList = new ArrayList<>();

        // 엘셀 파일 파싱
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            // index 0은 헤더이므로 index1 부터 시작
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String yearStr = getCellValue(row.getCell(0));
                    // 첫 번째 열(입학년도)이 비어있으면 데이터가 끝난 것으로 간주
                    if(yearStr.isEmpty()) break;

                    dtoList.add(ExcelStudent.builder()
                            .enrollmentYear(Integer.parseInt(yearStr))
                            .grade(Integer.parseInt(getCellValue(row.getCell(1))))
                            .classNum(Integer.parseInt(getCellValue(row.getCell(2))))
                            .studentNum(Integer.parseInt(getCellValue(row.getCell(3))))
                            .gender(parseGender(getCellValue(row.getCell(4))))
                            .studentName(getCellValue(row.getCell(5)))
                            .fatherPhone(getCellValue(row.getCell(6)))
                            .motherPhone(getCellValue(row.getCell(7)))
                            .build());
                } catch (Exception e) {
                    log.error("엑셀 파싱 에러 - Row: {}", i + 1, e);
                    throw new IllegalArgumentException((i + 1) + "번째 행의 데이터 형식이 잘못되었습니다");
                }
            }
            // 파싱 완료된 데이터 DB 일괄 저장
            saveExcelData(dtoList);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("엑셀 파일 읽기 실패", e);
            throw new RuntimeException("엑셀 파읽을 읽는 중 에러 발생");
        }
    }

    // DB 연관관계 맵핑 및 엔티티 생성 로직
    private void saveExcelData(List<ExcelStudent> dtoList) {
        for (ExcelStudent dto : dtoList) {
            // 해당 학년/반이 DB에 등록되어있는지 검증(없으면 에러, 엑셀 넣기 전 최고 관리자가 반 정보를 등록)
            Classroom classroom = entityManager.createQuery(
                    "SELECT c FROM Classroom c WHERE c.academicYear = :year AND c.grade = :grade AND c.classNum = :classNum", Classroom.class)
                    .setParameter("year", dto.getEnrollmentYear())
                    .setParameter("grade", dto.getGrade())
                    .setParameter("classNum", dto.getClassNum())
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            dto.getGrade() + "학년" + dto.getClassNum() + "반이 시스템에 등록되지 않았습니다"));

            // User 계정 생성 (아이디와 비밀번호는 추후 활성화 시 변경)
            User user = User.builder()
                    .name(dto.getStudentName())
                    .role(UserRole.STUDENT)
                    .status(UserStatus.PENDING)
                    .gender(dto.getGender())
                    .build();
            entityManager.persist(user);

            // Student 세부 정보 생성
            Student student = Student.builder()
                    .user(user)
                    .enrollmentYear(dto.getEnrollmentYear())
                    .build();
            entityManager.persist(student);

            // StudentAffiliation (학생-반 소속 정보) 생성
            StudentAffiliation affiliation = StudentAffiliation.builder()
                    .student(student)
                    .classroom(classroom)
                    .studentNum(dto.getStudentNum())
                    .build();
            entityManager.persist(affiliation);

            // 부모님 전화번호가 존재하면 초대장 발급
            if (isValidPhone(dto.getFatherPhone())) {
                // 부 전화번호 존재 시 가입 정보 생성
                createInvitation(student, dto.getFatherPhone(), RelationType.FATHER);
            }
            if (isValidPhone(dto.getMotherPhone())) {
                // 모 전화번호 존재 시 가입 정보 생성
                createInvitation(student, dto.getMotherPhone(), RelationType.MOTHER);
            }
        }

        // 50개를 카운트 해서 메모리를 비워주는 로직 추가하기
        // Batch Insert를 위해 영속성 컨텍스트를 한 번에 밀어내고 비움(지금 구조는 메모리 용량이 부족하면 문제 발생 개선 필요)
        entityManager.flush();
        entityManager.clear();
    }

    // 성별 정보 Enum 변환 메서드
    private Gender parseGender(String gender) {
        if ("남".equals(gender)) {
            return Gender.MALE;
        } else if ("여".equals(gender)) {
            return Gender.FEMALE;
        }
        // try-catch문 상 해당 에러문구는 못 감
        throw new IllegalArgumentException("성별은 남 또는 여만 입력 가능합니다, 입력된 값: " + gender);
    }

    // 부모 가입 정보 생성 메서드
    private void createInvitation(Student student, String phone, RelationType type) {
        ParentInvitation invitation = ParentInvitation.builder().
                student(student)
                .phoneNumber(phone.replace("-", ""))
                .relationType(type)
                .build();
        entityManager.persist(invitation);
    }

    // 전화번호 존재 여부 범증
    private boolean isValidPhone(String phone) {
        return phone != null && !phone.isBlank();
    }

    // 셀 데이터 타입 추출
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
