package com.poplavok.data.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import javax.annotation.Nullable;

public final class HibernateUtil {
    @Nullable
    private static SessionFactory SESSION_FACTORY;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (SESSION_FACTORY == null) {
            SESSION_FACTORY = buildSessionFactory();
        }
        return SESSION_FACTORY;
    }

    public static synchronized void closeSessionFactory() {
        if (SESSION_FACTORY != null) {
            SESSION_FACTORY.close();
            SESSION_FACTORY = null;
        }
    }

    public static void shutdown() {
        closeSessionFactory();
    }

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
}

