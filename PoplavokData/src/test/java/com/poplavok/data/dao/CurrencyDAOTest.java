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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
            try (Session session = sessionFactory.openSession()) {
                Transaction tx = session.beginTransaction();
                session.createMutationQuery("delete from Currency").executeUpdate();
                tx.commit();
            }
        }
    }

    @Test
    void testSave() {
        Currency currency = new Currency("BTC");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, currency);
            tx.commit();
        }

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
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, currency);
            tx.commit();
        }

        currency.setCurrency("ETH-UPDATED");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.update(session, currency);
            tx.commit();
        }

        try (Session session = sessionFactory.openSession()) {
             Currency found = session.find(Currency.class, currency.getId());
             assertEquals("ETH-UPDATED", found.getCurrency());
        }
    }

    @Test
    void testDelete() {
        Currency currency = new Currency("XRP");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, currency);
            tx.commit();
        }
        Long id = currency.getId();

        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Currency managedCurrency = session.find(Currency.class, id);
            CurrencyDAO.delete(session, managedCurrency);
            tx.commit();
        }

        try (Session session = sessionFactory.openSession()) {
             Currency found = session.find(Currency.class, id);
             assertNull(found);
        }
    }

    @Test
    void testFindById() {
        Currency currency = new Currency("LTC");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, currency);
            tx.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            Optional<Currency> found = CurrencyDAO.findById(session, currency.getId());
            assertTrue(found.isPresent());
            assertEquals("LTC", found.get().getCurrency());
        }

        try (Session session = sessionFactory.openSession()) {
            Optional<Currency> notFound = CurrencyDAO.findById(session, 999L);
            assertFalse(notFound.isPresent());
        }
    }

    @Test
    void testFindByName() {
        Currency currency = new Currency("DOGE");
        currency.setName("Dogecoin");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, currency);
            tx.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            Optional<Currency> found = CurrencyDAO.findByName(session, "Dogecoin");
            assertTrue(found.isPresent());
            assertEquals(currency.getId(), found.get().getId());
        }

        try (Session session = sessionFactory.openSession()) {
            Optional<Currency> notFound = CurrencyDAO.findByName(session, "NON_EXISTENT");
            assertFalse(notFound.isPresent());
        }
    }

    @Test
    void testFindAll() {
        Currency c1 = new Currency("C1");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, c1);
            tx.commit();
        }
        Currency c2 = new Currency("C2");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, c2);
            tx.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            List<Currency> all = CurrencyDAO.findAll(session);
            assertEquals(2, all.size());
        }
    }

    @Test
    void testNewFieldsPersistence() {
        Currency currency = new Currency("BTC_TEST");
        currency.setFullName("Bitcoin Test");
        currency.setPrecision(8);
        currency.setWithdrawalMinSize("0.001");
        currency.setWithdrawalMinFee("0.0005");
        currency.setIsWithdrawEnabled(true);
        currency.setIsDepositEnabled(true);
        currency.setIsMarginEnabled(false);
        currency.setIsDebitEnabled(false);

        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CurrencyDAO.save(session, currency);
            tx.commit();
        }

        try (Session session = sessionFactory.openSession()) {
            Currency found = session.find(Currency.class, currency.getId());
            assertNotNull(found);
            assertEquals("Bitcoin Test", found.getFullName());
            assertEquals(8, found.getPrecision());
            assertEquals("0.001", found.getWithdrawalMinSize());
            assertEquals("0.0005", found.getWithdrawalMinFee());
            assertEquals(Boolean.TRUE, found.getIsWithdrawEnabled());
            assertEquals(Boolean.TRUE, found.getIsDepositEnabled());
            assertEquals(Boolean.FALSE, found.getIsMarginEnabled());
            assertEquals(Boolean.FALSE, found.getIsDebitEnabled());
        }
    }
}
