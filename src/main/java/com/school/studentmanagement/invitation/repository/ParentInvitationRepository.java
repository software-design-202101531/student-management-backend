package com.school.studentmanagement.invitation.repository;

import com.school.studentmanagement.invitation.entity.ParentInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParentInvitationRepository extends JpaRepository<ParentInvitation, Long> {
    @Query("SELECT pi FROM ParentInvitation pi " +
            "JOIN pi.student s " +
            "JOIN s.user u " +
            "JOIN StudentAffiliation sa ON sa.student = s " +
            "JOIN sa.classroom c " +
            "WHERE c.academicYear = :year " +
            "AND c.grade = :grade " +
            "AND c.classNum = :classNum " +
            "AND sa.studentNum = :studentNum " +
            "AND u.name = :studentName " +
            "AND pi.phoneNumber = :parentPhone")
    Optional<ParentInvitation> findValidInvitation(
            @Param("year") Integer year,
            @Param("grade") Integer grade,
            @Param("classNum") Integer classNum,
            @Param("studentNum") Integer studentNum,
            @Param("studentName") String studentName,
            @Param("parentPhone") String parentPhone
    );

    Optional<ParentInvitation> findById(Long id);
}
