package com.poplavok.data.dao;

import com.poplavok.data.model.CurrencyExtendedInfo;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public class CurrencyExtendedInfoDAO {

    public static void save(Session session, CurrencyExtendedInfo info) {
        session.persist(info);
    }

    public static void update(Session session, CurrencyExtendedInfo info) {
        session.merge(info);
    }

    public static void delete(Session session, CurrencyExtendedInfo info) {
        session.remove(info);
    }

    public static Optional<CurrencyExtendedInfo> findById(Session session, Long id) {
        return Optional.ofNullable(session.find(CurrencyExtendedInfo.class, id));
    }

    public static List<CurrencyExtendedInfo> findAll(Session session) {
        return session.createQuery("from CurrencyExtendedInfo", CurrencyExtendedInfo.class).list();
    }
}

