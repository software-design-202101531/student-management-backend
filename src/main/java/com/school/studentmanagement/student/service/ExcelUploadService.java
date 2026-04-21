package com.school.studentmanagement.student.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.global.enums.Gender;
import com.school.studentmanagement.global.enums.RelationType;
import com.school.studentmanagement.global.enums.UserRole;
import com.school.studentmanagement.global.enums.UserStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.parent.entity.ParentInvitation;
import com.school.studentmanagement.student.dto.ExcelStudent;
import com.school.studentmanagement.student.entity.Student;
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
        if (file == null || file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        List<ExcelStudent> dtoList = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String yearStr = getCellValue(row.getCell(0));
                    if (yearStr.isEmpty()) break;

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
                } catch (BusinessException e) {
                    throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                            (i + 1) + "번째 행의 데이터 형식이 잘못되었습니다: " + e.getMessage());
                } catch (Exception e) {
                    log.error("엑셀 파싱 에러 - Row: {}", i + 1, e);
                    throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                            (i + 1) + "번째 행의 데이터 형식이 잘못되었습니다");
                }
            }
            saveExcelData(dtoList);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("엑셀 파일 읽기 실패", e);
            throw new BusinessException(ErrorCode.EXCEL_READ_ERROR);
        }
    }

    private void saveExcelData(List<ExcelStudent> dtoList) {
        for (ExcelStudent dto : dtoList) {
            Classroom classroom = entityManager.createQuery(
                    "SELECT c FROM Classroom c WHERE c.academicYear = :year AND c.grade = :grade AND c.classNum = :classNum",
                    Classroom.class)
                    .setParameter("year", dto.getEnrollmentYear())
                    .setParameter("grade", dto.getGrade())
                    .setParameter("classNum", dto.getClassNum())
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND,
                            dto.getGrade() + "학년 " + dto.getClassNum() + "반이 시스템에 등록되지 않았습니다"));

            User user = User.builder()
                    .name(dto.getStudentName())
                    .role(UserRole.STUDENT)
                    .status(UserStatus.PENDING)
                    .gender(dto.getGender())
                    .build();
            entityManager.persist(user);

            Student student = Student.builder()
                    .user(user)
                    .enrollmentYear(dto.getEnrollmentYear())
                    .build();
            entityManager.persist(student);

            StudentAffiliation affiliation = StudentAffiliation.builder()
                    .student(student)
                    .classroom(classroom)
                    .studentNum(dto.getStudentNum())
                    .build();
            entityManager.persist(affiliation);

            if (isValidPhone(dto.getFatherPhone())) {
                createInvitation(student, dto.getFatherPhone(), RelationType.FATHER);
            }
            if (isValidPhone(dto.getMotherPhone())) {
                createInvitation(student, dto.getMotherPhone(), RelationType.MOTHER);
            }
        }

        // Batch Insert를 위해 영속성 컨텍스트를 한 번에 밀어내고 비움(지금 구조는 메모리 용량이 부족하면 문제 발생 개선 필요)
        entityManager.flush();
        entityManager.clear();
    }

    private Gender parseGender(String gender) {
        if ("남".equals(gender)) return Gender.MALE;
        if ("여".equals(gender)) return Gender.FEMALE;
        throw new BusinessException(ErrorCode.INVALID_GENDER, "성별은 남 또는 여만 입력 가능합니다. 입력된 값: " + gender);
    }

    private void createInvitation(Student student, String phone, RelationType type) {
        ParentInvitation invitation = ParentInvitation.builder()
                .student(student)
                .phoneNumber(phone.replace("-", ""))
                .relationType(type)
                .build();
        entityManager.persist(invitation);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && !phone.isBlank();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
