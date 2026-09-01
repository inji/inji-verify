package io.inji.verify.repository;

import io.inji.verify.models.AuthorizationRequestCreateResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorizationRequestCreateResponseRepository extends JpaRepository<AuthorizationRequestCreateResponse, String> {
    List<AuthorizationRequestCreateResponse> findAllByTransactionIdOrderByExpiresAtDesc(@NotNull String transactionId);
}

