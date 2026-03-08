package com.poplavok.data.dao;

import com.poplavok.data.model.Currency;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MarketTickerDAOTest {

    private SessionFactory sessionFactory;
    private Currency baseCurrency;
    private Currency quoteCurrency;

    @BeforeEach
    void setUp() {
        sessionFactory = HibernateUtil.getSessionFactory();

        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            // Clean up dependent tables first
            session.createMutationQuery("delete from MarketTicker").executeUpdate();
            session.createMutationQuery("delete from Currency").executeUpdate();

            baseCurrency = new Currency("BTC");
            baseCurrency.setName("Bitcoin");
            quoteCurrency = new Currency("USD");
            quoteCurrency.setName("US Dollar");
            session.persist(baseCurrency);
            session.persist(quoteCurrency);

            tx.commit();
        }
    }

    @AfterEach
    void tearDown() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from MarketTicker").executeUpdate();
            session.createMutationQuery("delete from Currency").executeUpdate();
            tx.commit();
        }
    }

    @Test
    void testSave() {
        MarketTicker ticker = new MarketTicker(baseCurrency, quoteCurrency, "BTCUSD");
        MarketTickerDAO.save(sessionFactory, ticker);

        assertNotNull(ticker.getId());

        try (Session session = sessionFactory.openSession()) {
             MarketTicker found = session.find(MarketTicker.class, ticker.getId());
             assertNotNull(found);
             assertEquals("BTCUSD", found.getSymbol());
             assertEquals(baseCurrency.getId(), found.getBase().getId());
             assertEquals(quoteCurrency.getId(), found.getQuote().getId());
        }
    }

    @Test
    void testUpdate() {
        MarketTicker ticker = new MarketTicker(baseCurrency, quoteCurrency, "ETHUSD");
        MarketTickerDAO.save(sessionFactory, ticker);

        ticker.setSymbol("ETH-USD-UPDATED");
        MarketTickerDAO.update(sessionFactory, ticker);

        try (Session session = sessionFactory.openSession()) {
             MarketTicker found = session.find(MarketTicker.class, ticker.getId());
             assertEquals("ETH-USD-UPDATED", found.getSymbol());
        }
    }

    @Test
    void testDelete() {
        MarketTicker ticker = new MarketTicker(baseCurrency, quoteCurrency, "XRPUSD");
        MarketTickerDAO.save(sessionFactory, ticker);
        Long id = ticker.getId();

        MarketTickerDAO.delete(sessionFactory, ticker);

        try (Session session = sessionFactory.openSession()) {
             MarketTicker found = session.find(MarketTicker.class, id);
             assertNull(found);
        }
    }

    @Test
    void testFindById() {
        MarketTicker ticker = new MarketTicker(baseCurrency, quoteCurrency, "LTCUSD");
        MarketTickerDAO.save(sessionFactory, ticker);

        Optional<MarketTicker> found = MarketTickerDAO.findById(sessionFactory, ticker.getId());
        assertTrue(found.isPresent());
        assertEquals("LTCUSD", found.get().getSymbol());
    }

    @Test
    void testFindBySymbol() {
        MarketTicker ticker = new MarketTicker(baseCurrency, quoteCurrency, "DOGEUSD");
        MarketTickerDAO.save(sessionFactory, ticker);

        Optional<MarketTicker> found = MarketTickerDAO.findBySymbol(sessionFactory, "DOGEUSD");
        assertTrue(found.isPresent());
        assertEquals(ticker.getId(), found.get().getId());
    }

    @Test
    void testFields() {
        MarketTicker ticker = new MarketTicker(baseCurrency, quoteCurrency, "TEST");
        ticker.setSymbolName("Test Symbol");
        ticker.setTakerFeeRate("0.01");
        ticker.setMakerFeeRate("0.02");
        ticker.setTakerCoefficient("1.1");
        ticker.setMakerCoefficient("1.2");

        MarketTickerDAO.save(sessionFactory, ticker);

        try (Session session = sessionFactory.openSession()) {
            MarketTicker found = session.find(MarketTicker.class, ticker.getId());
            assertEquals("Test Symbol", found.getSymbolName());
            assertEquals("0.01", found.getTakerFeeRate());
            assertEquals("0.02", found.getMakerFeeRate());
            assertEquals("1.1", found.getTakerCoefficient());
            assertEquals("1.2", found.getMakerCoefficient());
        }
    }

    @Test
    void testFindAll() {
        MarketTicker ticker1 = new MarketTicker(baseCurrency, quoteCurrency, "T1");
        MarketTickerDAO.save(sessionFactory, ticker1);

        MarketTicker ticker2 = new MarketTicker(baseCurrency, quoteCurrency, "T2");
        MarketTickerDAO.save(sessionFactory, ticker2);

        List<MarketTicker> all = MarketTickerDAO.findAll(sessionFactory);
        assertEquals(2, all.size());
    }
}

