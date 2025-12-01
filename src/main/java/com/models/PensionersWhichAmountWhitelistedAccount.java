package com.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Setter
@Getter
@Entity
@Table(name = "pensioners_which_amount_whitelisted_account")
public class PensionersWhichAmountWhitelistedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String account;

    private Timestamp logTs;

    @PrePersist
    protected void onCreate() {
        if (logTs == null) {
            logTs = new Timestamp(System.currentTimeMillis());
        }
    }
}
