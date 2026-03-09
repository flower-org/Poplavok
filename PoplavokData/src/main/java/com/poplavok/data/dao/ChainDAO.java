package com.poplavok.data.dao;

import com.poplavok.data.model.Chain;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class ChainDAO {

    public static void save(Session session, Chain chain) {
        session.persist(chain);
    }

    public static void update(Session session, Chain chain) {
        session.merge(chain);
    }

    public static void delete(Session session, Chain chain) {
        session.remove(chain);
    }

    @Nullable
    public static Chain getById(Session session, Long id) {
        return session.find(Chain.class, id);
    }

    public static Optional<Chain> findById(Session session, Long id) {
        return Optional.ofNullable(session.find(Chain.class, id));
    }

    @Nullable
    public static Chain getByChain(Session session, String chain) {
        Query<Chain> query = session.createQuery("from Chain where chain = :chain", Chain.class);
        query.setParameter("chain", chain);
        return query.uniqueResult();
    }

    public static List<Chain> findAll(Session session) {
        return session.createQuery("from Chain", Chain.class).list();
    }
}

