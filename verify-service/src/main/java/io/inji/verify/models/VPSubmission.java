package io.inji.verify.models;

import java.sql.Timestamp;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "vp_submission")
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class VPSubmission {
    @Id
    @JsonProperty("state")
    @SerializedName("state")
    private final String requestId;

    @JdbcTypeCode(SqlTypes.CLOB)
    private final String vpToken;

    private final String presentationSubmission;

    private final String error;

    private final String errorDescription;

    private final String responseCode;

    private final Timestamp responseCodeExpiryAt;

    private final boolean responseCodeUsed;
}