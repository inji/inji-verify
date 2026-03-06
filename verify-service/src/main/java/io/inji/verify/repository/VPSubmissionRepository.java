package io.inji.verify.repository;

import io.inji.verify.models.VPSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VPSubmissionRepository extends JpaRepository<VPSubmission, String> {
}