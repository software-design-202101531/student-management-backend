package com.school.studentmanagement.global.enums;

public enum GradeLevel {
    A, B, C, D, E;

    public static GradeLevel from(double averageScore) {
        if (averageScore >= 90) return A;
        if (averageScore >= 80) return B;
        if (averageScore >= 70) return C;
        if (averageScore >= 60) return D;
        return E;
    }
}
