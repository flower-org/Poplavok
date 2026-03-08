package com.poplavok.data.dao;

import com.poplavok.data.model.Currency;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class CurrencyDAO {

    public static void save(Session session, Currency currency) {
        session.persist(currency);
    }

    public static void update(Session session, Currency currency) {
        session.merge(currency);
    }

    public static void delete(Session session, Currency currency) {
        session.remove(currency);
    }

    public static Optional<Currency> findById(Session session, Long id) {
        return Optional.ofNullable(session.find(Currency.class, id));
    }

    public static Optional<Currency> findByName(Session session, String name) {
        Query<Currency> query = session.createQuery("from Currency where name = :name", Currency.class);
        query.setParameter("name", name);
        return query.uniqueResultOptional();
    }

    public static List<Currency> findAll(Session session) {
        return session.createQuery("from Currency", Currency.class).list();
    }
}

