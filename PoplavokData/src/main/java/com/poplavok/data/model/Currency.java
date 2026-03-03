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
    @Nullable
    private String name;

    @OneToMany(mappedBy = "currency")
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "currency")
    private List<Loan> loans = new ArrayList<>();

    public Currency() {
    }

    public Currency(String name) {
        this.name = name;
    }

    public Long getId() {
        return checkNotNull(id);
    }

    public String getName() {
        return checkNotNull(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Loan> getLoans() {
        return loans;
    }
}

