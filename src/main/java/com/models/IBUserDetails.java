package com.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IBUserDetails {
    String userName;
    String fullName;
    String email;
    String contactNumber;
    int accountExpires;
    int credentialsExpires;
    int enableAccount;
    int accountLocked;
}
