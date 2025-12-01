package com.DTO.salary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.*;
import java.util.List;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TRANSACTIONS")
public class TRANSACTIONS {
    @XmlElement(name = "TRANSACTION", required = true)
    private List<TRANSACTION> TRANSACTION;

    public List<com.DTO.salary.TRANSACTION> getTRANSACTION() {
        return TRANSACTION;
    }

    public void setTRANSACTION(List<com.DTO.salary.TRANSACTION> TRANSACTION) {
        this.TRANSACTION = TRANSACTION;
    }

    @Override
    public String toString() {
        return "TRANSACTIONS{" +
                "TRANSACTION=" + TRANSACTION +
                '}';
    }
}
