package com.poplavok.data.dao;

import com.poplavok.data.model.Chain;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.CurrencyChain;
import com.poplavok.data.model.CurrencyChainEx;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CurrencyChainDAO {

    public static void save(Session session, CurrencyChain chain) {
        session.persist(chain);
    }

    public static void update(Session session, CurrencyChain chain) {
        session.merge(chain);
    }

    public static void delete(Session session, CurrencyChain chain) {
        session.remove(chain);
    }

    public static Optional<CurrencyChain> findById(Session session, Long id) {
        return Optional.ofNullable(session.find(CurrencyChain.class, id));
    }

    public static List<CurrencyChain> findAll(Session session) {
        return session.createQuery("from CurrencyChain", CurrencyChain.class).list();
    }

    public static List<CurrencyChainEx> getForCurrencyEx(Session session, Long currencyId) {
        List<CurrencyChain> chains = session.createQuery("from CurrencyChain where currencyId = :currencyId", CurrencyChain.class)
                .setParameter("currencyId", currencyId)
                .list();

        // Fetch currency name
        String currencyName = Optional.ofNullable(session.find(Currency.class, currencyId))
                .map(Currency::getName)
                .orElse(null);

        List<CurrencyChainEx> result = new ArrayList<>();
        for (CurrencyChain chain : chains) {
            CurrencyChainEx ex = new CurrencyChainEx(chain.getCurrencyId(), chain.getChainId());
            ex.setId(chain.getId());
            ex.setWithdrawalMinSize(chain.getWithdrawalMinSize());
            ex.setWithdrawalMinFee(chain.getWithdrawalMinFee());
            ex.setIsWithdrawEnabled(chain.getIsWithdrawEnabled());
            ex.setIsDepositEnabled(chain.getIsDepositEnabled());
            ex.setConfirms(chain.getConfirms());
            ex.setContractAddress(chain.getContractAddress());

            ex.setCurrency(currencyName);

            // Fetch Chain Entity
            Long chainId = chain.getChainId();
            String chainName = null;
            if (chainId != null) {
                Chain chainEntity = session.find(Chain.class, chainId);
                if (chainEntity != null) {
                    chainName = chainEntity.getChain(); // Assuming getChain() returns the string
                    if (chainName == null) {
                        chainName = chainEntity.getChainName();
                    }
                }
            }
            ex.setChain(chainName != null ? chainName : (chainId != null ? String.valueOf(chainId) : null));

            result.add(ex);
        }
        return result;
    }

    public static List<CurrencyChain> getForCurrency(Session session, Long currencyId) {
        return session.createQuery("from CurrencyChain where currencyId = :currencyId", CurrencyChain.class)
                .setParameter("currencyId", currencyId)
                .list();
    }
}
