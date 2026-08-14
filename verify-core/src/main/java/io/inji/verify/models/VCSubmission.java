package io.inji.verify.models;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Table(name = "vc_submission")
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class VCSubmission {
    @Id
    @Column(name = "transaction_id")
    String transactionId;

    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "vc")
    String vc;
}