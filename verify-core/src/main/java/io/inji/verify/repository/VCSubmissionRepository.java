package io.inji.verify.repository;

import io.inji.verify.models.VCSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VCSubmissionRepository extends JpaRepository<VCSubmission, String> {
}
