package com.school.studentmanagement.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(400, "INVALID_INPUT", "잘못된 입력입니다"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    ACCESS_DENIED(403, "ACCESS_DENIED", "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"),

    // 인증/계정
    LOGIN_FAILED(401, "LOGIN_FAILED", "아이디 혹은 비밀번호가 올바르지 않습니다"),
    ACCOUNT_NOT_ACTIVE(403, "ACCOUNT_NOT_ACTIVE", "활성화 되지 않은 계정입니다. 관리자에게 문의하세요"),
    ACCOUNT_ALREADY_ACTIVE(409, "ACCOUNT_ALREADY_ACTIVE", "이미 활성화된 계정입니다"),

    // 사용자
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "유효하지 않은 사용자입니다"),
    STUDENT_NOT_FOUND(404, "STUDENT_NOT_FOUND", "유효하지 않은 학생 정보입니다"),
    STUDENT_VERIFY_FAILED(400, "STUDENT_VERIFY_FAILED", "입력하신 정보와 일치하는 가입 대기 정보가 없습니다"),
    TEACHER_NOT_FOUND(404, "TEACHER_NOT_FOUND", "선생님 정보를 찾을 수 없습니다"),

    // 반/소속
    CLASSROOM_NOT_FOUND(404, "CLASSROOM_NOT_FOUND", "반 정보가 존재하지 않습니다"),
    HOMEROOM_NOT_FOUND(404, "HOMEROOM_NOT_FOUND", "담임을 담당한 반이 없습니다"),
    STUDENT_NOT_IN_CLASSROOM(400, "STUDENT_NOT_IN_CLASSROOM", "해당 반의 학생이 아닙니다"),
    STUDENT_NOT_ASSIGNED(404, "STUDENT_NOT_ASSIGNED", "해당 학기에 배정된 학생이 아닙니다"),

    // 성적
    GRADE_NOT_FOUND(404, "GRADE_NOT_FOUND", "성적 정보를 찾을 수 없습니다"),
    EXAM_NOT_FOUND(404, "EXAM_NOT_FOUND", "해당 시험 정보를 찾을 수 없습니다"),
    GRADE_SUBJECT_MISMATCH(400, "GRADE_SUBJECT_MISMATCH", "성적 과목 정보가 일치하지 않습니다"),
    EXAM_NAME_DUPLICATED(409, "EXAM_NAME_DUPLICATED", "같은 학기에 동일한 이름의 시험이 이미 존재합니다"),
    EXAM_ALREADY_PUBLISHED(409, "EXAM_ALREADY_PUBLISHED", "이미 공개된 시험입니다"),
    EXAM_NOT_PUBLISHED(403, "EXAM_NOT_PUBLISHED", "아직 공개되지 않은 시험 정보입니다"),
    EXAM_SCORE_OUT_OF_RANGE(400, "EXAM_SCORE_OUT_OF_RANGE", "허용된 점수 범위를 벗어났습니다"),
    SEMESTER_ALREADY_CLOSED(409, "SEMESTER_ALREADY_CLOSED", "이미 마감된 학기입니다"),
    SEMESTER_NOT_CLOSED(409, "SEMESTER_NOT_CLOSED", "마감되지 않은 학기입니다"),
    SEMESTER_CLOSED(403, "SEMESTER_CLOSED", "마감된 학기는 수정할 수 없습니다"),

    // 출결
    ATTENDANCE_FUTURE_DATE(400, "ATTENDANCE_FUTURE_DATE", "출결은 미리 입력할 수 없습니다"),

    // 기록 (행특, 과세특)
    RECORD_DEADLINE_EXCEEDED(400, "RECORD_DEADLINE_EXCEEDED", "해당 학년도의 기록 작성 및 수정 기간은 마감되었습니다"),

    // 피드백
    FEEDBACK_NOT_FOUND(404, "FEEDBACK_NOT_FOUND", "피드백 정보를 찾을 수 없습니다"),
    FEEDBACK_ALREADY_PUBLISHED(409, "FEEDBACK_ALREADY_PUBLISHED", "이미 발행된 피드백입니다"),

    // 상담
    CONSULTATION_NOT_FOUND(404, "CONSULTATION_NOT_FOUND", "상담 내역을 찾을 수 없습니다"),

    // 학부모
    PARENT_VERIFY_FAILED(400, "PARENT_VERIFY_FAILED", "입력하신 정보가 올바르지 않습니다"),
    INVITATION_NOT_FOUND(400, "INVITATION_NOT_FOUND", "유효하지 않은 초대 정보입니다"),

    // 엑셀
    INVALID_FILE(400, "INVALID_FILE", "올바른 엑셀 파일(.xlsx)을 업로드 해주세요"),
    EXCEL_PARSE_ERROR(400, "EXCEL_PARSE_ERROR", "엑셀 데이터 형식이 잘못되었습니다"),
    EXCEL_READ_ERROR(500, "EXCEL_READ_ERROR", "엑셀 파일을 읽는 중 오류가 발생했습니다"),
    INVALID_GENDER(400, "INVALID_GENDER", "성별은 남 또는 여만 입력 가능합니다");

    private final int status;
    private final String code;
    private final String message;
}
