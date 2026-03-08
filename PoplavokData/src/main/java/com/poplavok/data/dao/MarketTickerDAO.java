package com.poplavok.data.dao;

import com.poplavok.data.model.Currency;
import com.poplavok.data.model.MarketTicker;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class MarketTickerDAO {

    public static void save(Session session, MarketTicker marketTicker) {
        session.persist(marketTicker);
    }

    public static void update(Session session, MarketTicker marketTicker) {
        session.merge(marketTicker);
    }

    public static void delete(Session session, MarketTicker marketTicker) {
        session.remove(marketTicker);
    }

    public static Optional<MarketTicker> findById(Session session, Long id) {
        return Optional.ofNullable(session.find(MarketTicker.class, id));
    }

    public static Optional<MarketTicker> findByCurrencies(Session session, Currency base, Currency quote) {
        Query<MarketTicker> query = session.createQuery("from MarketTicker where base = :base and quote = :quote", MarketTicker.class);
        query.setParameter("base", base);
        query.setParameter("quote", quote);
        return query.uniqueResultOptional();
    }

    public static List<MarketTicker> findAll(Session session) {
        return session.createQuery("from MarketTicker", MarketTicker.class).list();
    }

    public static Optional<MarketTicker> findBySymbol(Session session, String symbol) {
        Query<MarketTicker> query = session.createQuery("from MarketTicker where symbol = :symbol", MarketTicker.class);
        query.setParameter("symbol", symbol);
        return query.uniqueResultOptional();
    }
}
