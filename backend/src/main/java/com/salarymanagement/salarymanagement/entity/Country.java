package com.salarymanagement.salarymanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "country")
public class Country {

    @Id
    @Column(name = "code", length = 10, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    public Country() {
    }

    public Country(String code, String name, String currencyCode) {
        this.code = code;
        this.name = name;
        this.currencyCode = currencyCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Country country)) return false;
        return code != null && code.equals(country.getCode());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
