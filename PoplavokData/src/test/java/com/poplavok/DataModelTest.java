package com.poplavok;

import com.poplavok.data.config.HibernateUtil;
import com.poplavok.data.model.Account;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.Level;
import com.poplavok.data.model.LevelStrategy;
import com.poplavok.data.model.Loan;
import com.poplavok.data.model.LoanType;
import com.poplavok.data.model.OperationHistory;
import com.poplavok.data.model.OperationType;
import com.poplavok.data.model.Poplavok;
import com.poplavok.data.model.Repayment;
import com.poplavok.data.model.Ticker;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataModelTest {

    private Session session;

    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
    }

    @AfterEach
    void tearDown() {
        if (session != null && session.isOpen()) {
            session.close();
        }
        HibernateUtil.closeSessionFactory();
    }

    @Test
    public void testCurrencyAndTicker() {
        Transaction tx = session.beginTransaction();

        Currency btc = new Currency("BTC");
        Currency usdt = new Currency("USDT");
        session.persist(btc);
        session.persist(usdt);

        Ticker ticker = new Ticker(btc, usdt, new BigDecimal("0.001"));
        session.persist(ticker);

        tx.commit();
        session.clear();

        List<Ticker> tickers = session
                .createQuery("from Ticker", Ticker.class)
                .list();

        assertEquals(1, tickers.size());
        assertEquals("BTC", tickers.get(0).getBase().getName());
        assertEquals("USDT", tickers.get(0).getQuote().getName());
    }

    @Test
    public void testPoplavokWithLevels() {
        Transaction tx = session.beginTransaction();

        Currency btc = new Currency("BTC");
        Currency usdt = new Currency("USDT");
        session.persist(btc);
        session.persist(usdt);

        Ticker ticker = new Ticker(btc, usdt, new BigDecimal("0.001"));
        session.persist(ticker);

        Poplavok poplavok = new Poplavok(
                ticker,
                LevelStrategy.LINEAR,
                "{}",
                new BigDecimal("50000"),
                LocalDateTime.now()
        );
        session.persist(poplavok);

        Level level1 = new Level(1, poplavok, new BigDecimal("0.1"), new BigDecimal("5000"),
                new BigDecimal("50000"), LocalDateTime.now());
        Level level2 = new Level(2, poplavok, new BigDecimal("0.2"), new BigDecimal("9500"),
                new BigDecimal("47500"), LocalDateTime.now());
        poplavok.addLevel(level1);
        poplavok.addLevel(level2);

        tx.commit();
        session.clear();

        List<Poplavok> poplavoks = session
                .createQuery("from Poplavok", Poplavok.class)
                .list();

        assertEquals(1, poplavoks.size());
        assertEquals(2, poplavoks.get(0).getLevels().size());
        assertTrue(poplavoks.get(0).isActive());
    }

    @Test
    public void testLoanAndRepayment() {
        Transaction tx = session.beginTransaction();

        Currency usdt = new Currency("USDT");
        session.persist(usdt);

        Loan loan = new Loan(usdt, new BigDecimal("1000"), null, null,
                LocalDateTime.now(), LoanType.EXTERNAL);
        session.persist(loan);

        Repayment repayment = new Repayment(loan, new BigDecimal("500"), LocalDateTime.now());
        loan.addRepayment(repayment);

        tx.commit();
        session.clear();

        List<Loan> loans = session
                .createQuery("from Loan", Loan.class)
                .list();

        assertEquals(1, loans.size());
        assertEquals(1, loans.get(0).getRepayments().size());
        assertEquals(new BigDecimal("500.00000000"), loans.get(0).getRepayments().get(0).getAmount());
    }

    @Test
    public void testAccountAndHistory() {
        Transaction tx = session.beginTransaction();

        Currency btc = new Currency("BTC");
        session.persist(btc);

        Account account = new Account(btc, new BigDecimal("10"), BigDecimal.ZERO);
        session.persist(account);

        tx.commit();
        session.clear();

        List<Account> accounts = session
                .createQuery("from Account", Account.class)
                .list();

        assertEquals(1, accounts.size());
        assertEquals("BTC", accounts.get(0).getCurrency().getName());
    }

    @Test
    public void testOperationHistory() {
        Transaction tx = session.beginTransaction();

        Currency btc = new Currency("BTC");
        Currency usdt = new Currency("USDT");
        session.persist(btc);
        session.persist(usdt);

        Ticker ticker = new Ticker(btc, usdt, new BigDecimal("0.001"));
        session.persist(ticker);

        Poplavok poplavok = new Poplavok(
                ticker,
                LevelStrategy.EXPONENTIAL,
                "{}",
                new BigDecimal("60000"),
                LocalDateTime.now()
        );
        session.persist(poplavok);

        // Create a level since OperationHistory requires non-null level
        Level level = new Level(1, poplavok, new BigDecimal("0.1"), new BigDecimal("5000"),
                new BigDecimal("60000"), LocalDateTime.now());
        poplavok.addLevel(level);
        session.persist(level);

        OperationHistory operation = new OperationHistory(
                poplavok, level, OperationType.CREATION,
                new BigDecimal("1.0"), new BigDecimal("60000"), LocalDateTime.now(),
                "Poplavok created"
        );
        poplavok.addOperationHistory(operation);

        tx.commit();
        session.clear();

        List<OperationHistory> operations = session
                .createQuery("from OperationHistory", OperationHistory.class)
                .list();

        assertEquals(1, operations.size());
        assertEquals(OperationType.CREATION, operations.get(0).getOperationType());
        assertNotNull(operations.get(0).getPoplavok());
    }
}

