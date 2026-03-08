package com.poplavok.data.dao;

import com.poplavok.data.model.Currency;
import com.poplavok.data.model.MarketTicker;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class MarketTickerDAO {

    public static void save(SessionFactory sessionFactory, MarketTicker marketTicker) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(marketTicker);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static void update(SessionFactory sessionFactory, MarketTicker marketTicker) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(marketTicker);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static void delete(SessionFactory sessionFactory, MarketTicker marketTicker) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.remove(marketTicker);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public static Optional<MarketTicker> findById(SessionFactory sessionFactory, Long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(MarketTicker.class, id));
        }
    }

    public static Optional<MarketTicker> findByCurrencies(SessionFactory sessionFactory, Currency base, Currency quote) {
        try (Session session = sessionFactory.openSession()) {
            Query<MarketTicker> query = session.createQuery("from MarketTicker where base = :base and quote = :quote", MarketTicker.class);
            query.setParameter("base", base);
            query.setParameter("quote", quote);
            return query.uniqueResultOptional();
        }
    }

    public static List<MarketTicker> findAll(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from MarketTicker", MarketTicker.class).list();
        }
    }

    public static Optional<MarketTicker> findBySymbol(SessionFactory sessionFactory, String symbol) {
        try (Session session = sessionFactory.openSession()) {
            Query<MarketTicker> query = session.createQuery("from MarketTicker where symbol = :symbol", MarketTicker.class);
            query.setParameter("symbol", symbol);
            return query.uniqueResultOptional();
        }
    }
}
