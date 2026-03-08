package com.poplavok.data.dao;

import com.poplavok.data.model.Currency;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class CurrencyDAO {

    public static void save(SessionFactory sessionFactory, Currency currency) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(currency);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static void update(SessionFactory sessionFactory, Currency currency) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(currency);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static void delete(SessionFactory sessionFactory, Currency currency) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.remove(currency);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static Optional<Currency> findById(SessionFactory sessionFactory, Long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(Currency.class, id));
        }
    }

    public static Optional<Currency> findByName(SessionFactory sessionFactory, String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Currency> query = session.createQuery("from Currency where name = :name", Currency.class);
            query.setParameter("name", name);
            return query.uniqueResultOptional();
        }
    }

    public static List<Currency> findAll(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Currency", Currency.class).list();
        }
    }
}

