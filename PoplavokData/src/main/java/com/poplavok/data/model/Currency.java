package com.poplavok.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "currencies")
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String currency;

    @Column
    @Nullable
    private String name;

    @Column
    @Nullable
    private String fullName;



    @OneToMany(mappedBy = "currency")
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "currency")
    private List<Loan> loans = new ArrayList<>();

    public Currency() {
        this.currency = "";
    }

    public Currency(String currency) {
        this.currency = currency;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public String getCurrency() {
        return checkNotNull(currency);
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getFullName() {
        return fullName;
    }

    public void setFullName(@Nullable String fullName) {
        this.fullName = fullName;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Loan> getLoans() {
        return loans;
    }
}
