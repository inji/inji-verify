package io.inji.verify.repository;

import io.inji.verify.models.VPSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface VPSubmissionRepository extends JpaRepository<VPSubmission, String> {

	/**
	 * Atomically marks response_code_used as true for a given response_code if not already used.
	 * Returns the number of rows updated (should be 1 if successful, 0 if already used or not found).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query(value = "UPDATE vp_submission SET response_code_used = true " +
			"WHERE response_code = :responseCode AND response_code_used = false",
			nativeQuery = true)
	int setResponseCodeAsUsed(String responseCode);

	Optional<VPSubmission> findByResponseCode(String responseCode);
}