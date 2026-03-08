package com.poplavok.data.dao;

import com.poplavok.data.utils.HibernateUtil;
import com.poplavok.data.model.Currency;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyDAOTest {

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        sessionFactory = HibernateUtil.getSessionFactory();

        // Ensure clean state before each test
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Currency").executeUpdate();
            tx.commit();
        }
    }

    @AfterEach
    void tearDown() {
        if (sessionFactory != null) {
            // We don't close sessionFactory here because it is a static singleton in HibernateUtil
            // and might be reused or closed by the test framework if needed.
            // But DataModelTest closes it in tearDown?
            // Let's check DataModelTest again.
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Currency").executeUpdate();
            tx.commit();
        }
    }

    @Test
    void testSave() {
        Currency currency = new Currency("BTC");
        CurrencyDAO.save(sessionFactory, currency);

        assertNotNull(currency.getId());

        try (Session session = sessionFactory.openSession()) {
             Currency found = session.find(Currency.class, currency.getId());
             assertNotNull(found);
             assertEquals("BTC", found.getCurrency());
        }
    }

    @Test
    void testUpdate() {
        Currency currency = new Currency("ETH");
        CurrencyDAO.save(sessionFactory, currency);

        currency.setCurrency("ETH-UPDATED");
        CurrencyDAO.update(sessionFactory, currency);

        try (Session session = sessionFactory.openSession()) {
             Currency found = session.find(Currency.class, currency.getId());
             assertEquals("ETH-UPDATED", found.getCurrency());
        }
    }

    @Test
    void testDelete() {
        Currency currency = new Currency("XRP");
        CurrencyDAO.save(sessionFactory, currency);
        Long id = currency.getId();

        CurrencyDAO.delete(sessionFactory, currency);

        try (Session session = sessionFactory.openSession()) {
             Currency found = session.find(Currency.class, id);
             assertNull(found);
        }
    }

    @Test
    void testFindById() {
        Currency currency = new Currency("LTC");
        CurrencyDAO.save(sessionFactory, currency);

        Optional<Currency> found = CurrencyDAO.findById(sessionFactory, currency.getId());
        assertTrue(found.isPresent());
        assertEquals("LTC", found.get().getCurrency());

        Optional<Currency> notFound = CurrencyDAO.findById(sessionFactory, 999L);
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindByName() {
        Currency currency = new Currency("DOGE");
        currency.setName("Dogecoin");
        CurrencyDAO.save(sessionFactory, currency);

        Optional<Currency> found = CurrencyDAO.findByName(sessionFactory, "Dogecoin");
        assertTrue(found.isPresent());
        assertEquals(currency.getId(), found.get().getId());

        Optional<Currency> notFound = CurrencyDAO.findByName(sessionFactory, "NON_EXISTENT");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindAll() {
        CurrencyDAO.save(sessionFactory, new Currency("C1"));
        CurrencyDAO.save(sessionFactory, new Currency("C2"));

        List<Currency> all = CurrencyDAO.findAll(sessionFactory);
        assertEquals(2, all.size());
    }
}







