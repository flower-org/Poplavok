package com.poplavok.data;

import com.poplavok.data.utils.HibernateUtil;
import com.poplavok.data.model.Account;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.Level;
import com.poplavok.data.model.LevelStrategy;
import com.poplavok.data.model.Loan;
import com.poplavok.data.model.LoanType;
import com.poplavok.data.model.OperationHistory;
import com.poplavok.data.model.OperationType;
import com.poplavok.data.model.Poplavok;
import com.poplavok.data.model.MarketTicker;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class DemoApp {
    public static void main(String[] args) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        // Create currencies
        Currency btc = new Currency("BTC");
        Currency usdt = new Currency("USDT");
        session.persist(btc);
        session.persist(usdt);

        // Create ticker BTC/USDT
        MarketTicker btcUsdt = new MarketTicker(btc, usdt, "BTCUSDT");
        btcUsdt.setTakerFeeRate("0.001");
        btcUsdt.setMakerFeeRate("0.001");
        session.persist(btcUsdt);

        // Create accounts
        Account btcAccount = new Account(btc, new BigDecimal("1.5"), BigDecimal.ZERO);
        Account usdtAccount = new Account(usdt, new BigDecimal("10000"), BigDecimal.ZERO);
        session.persist(btcAccount);
        session.persist(usdtAccount);

        // Create a Poplavok
        Poplavok poplavok = new Poplavok(
                btcUsdt,
                LevelStrategy.LINEAR,
                "{\"step\": 0.05, \"multiplier\": 1.5}",
                new BigDecimal("45000"),
                LocalDateTime.now()
        );
        session.persist(poplavok);

        // Create levels
        Level level1 = new Level(1, poplavok, new BigDecimal("0.1"), new BigDecimal("4500"),
                new BigDecimal("45000"), LocalDateTime.now());
        Level level2 = new Level(2, poplavok, new BigDecimal("0.15"), new BigDecimal("6412.5"),
                new BigDecimal("42750"), LocalDateTime.now());
        poplavok.addLevel(level1);
        poplavok.addLevel(level2);

        // Create a loan for level 2
        Loan loan = new Loan(usdt, new BigDecimal("6412.5"), poplavok, level2,
                LocalDateTime.now(), LoanType.POPLAVOK_FUNDED);
        poplavok.addLoan(loan);
        level2.addLoan(loan);
        session.persist(loan);

        // Add operation history
        OperationHistory creation = new OperationHistory(
                poplavok, level2, OperationType.CREATION,
                new BigDecimal("35000"), new BigDecimal("45000"), LocalDateTime.now(),
                "Poplavok created with entry price 45000"
        );
        OperationHistory buy = new OperationHistory(
                poplavok, level1, OperationType.BUY,
                new BigDecimal("0.1"), new BigDecimal("45000"), LocalDateTime.now(),
                "Bought 0.1 BTC at level 1"
        );
        poplavok.addOperationHistory(creation);
        poplavok.addOperationHistory(buy);

        tx.commit();
        session.close();

        System.out.println("Demo data created successfully!");
        System.out.println("Created currencies: BTC, USDT");
        System.out.println("Created ticker: BTC/USDT");
        System.out.println("Created Poplavok with 2 levels");

        HibernateUtil.shutdown();
    }
}

