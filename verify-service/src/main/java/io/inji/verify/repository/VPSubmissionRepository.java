package io.inji.verify.repository;

import io.inji.verify.models.VPSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VPSubmissionRepository extends JpaRepository<VPSubmission, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VPSubmission s SET s.responseCodeUsed = true WHERE s.requestId = :requestId AND s.responseCodeUsed = false")
    int markResponseCodeAsUsed(@Param("requestId") String requestId);
}