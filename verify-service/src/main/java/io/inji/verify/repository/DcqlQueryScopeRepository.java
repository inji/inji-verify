package io.inji.verify.repository;

import io.inji.verify.models.DcqlQueryScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DcqlQueryScopeRepository extends JpaRepository<DcqlQueryScope, String> {
}
