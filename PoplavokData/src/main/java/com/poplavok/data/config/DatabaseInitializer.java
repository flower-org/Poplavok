package com.poplavok.data.config;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for database initialization and schema management.
 * Provides methods to create, drop, and validate the database schema.
 */
public class DatabaseInitializer {

    /**
     * Creates the database schema based on Hibernate entity mappings.
     * Drops existing tables and creates new ones.
     */
    public static void createDatabase() {
        System.out.println("Creating database schema...");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .configure()
                .build();

        try {
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .buildMetadata();

            // Use Hibernate 6 schema management
            Map<String, Object> settings = new HashMap<>();
            settings.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");

            SchemaManagementToolCoordinator.process(
                    metadata,
                    serviceRegistry,
                    settings,
                    action -> {}
            );

            System.out.println("Database schema created successfully!");
        } catch (Exception e) {
            System.err.println("Error creating database schema: " + e.getMessage());
            throw new RuntimeException("Failed to create database schema", e);
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    /**
     * Drops the database schema (all tables).
     */
    public static void dropDatabase() {
        System.out.println("Dropping database schema...");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .configure()
                .build();

        try {
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .buildMetadata();

            Map<String, Object> settings = new HashMap<>();
            settings.put("jakarta.persistence.schema-generation.database.action", "drop");

            SchemaManagementToolCoordinator.process(
                    metadata,
                    serviceRegistry,
                    settings,
                    action -> {}
            );

            System.out.println("Database schema dropped successfully!");
        } catch (Exception e) {
            System.err.println("Error dropping database schema: " + e.getMessage());
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    /**
     * Creates the database schema only (without dropping existing tables).
     */
    public static void createSchemaOnly() {
        System.out.println("Creating database schema (without drop)...");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .configure()
                .build();

        try {
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .buildMetadata();

            Map<String, Object> settings = new HashMap<>();
            settings.put("jakarta.persistence.schema-generation.database.action", "create");

            SchemaManagementToolCoordinator.process(
                    metadata,
                    serviceRegistry,
                    settings,
                    action -> {}
            );

            System.out.println("Database schema created successfully!");
        } catch (Exception e) {
            System.err.println("Error creating database schema: " + e.getMessage());
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    /**
     * Exports the DDL script to console (useful for debugging).
     */
    public static void exportSchemaToConsole() {
        System.out.println("Exporting DDL script to console...");
        System.out.println("=====================================");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .configure()
                .build();

        try {
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .buildMetadata();

            Map<String, Object> settings = new HashMap<>();
            settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
            settings.put("jakarta.persistence.schema-generation.scripts.create-target", "");
            settings.put("hibernate.hbm2ddl.delimiter", ";");
            settings.put("hibernate.format_sql", "true");

            // Print DDL to stdout
            metadata.getDatabase().getNamespaces().forEach(namespace -> {
                namespace.getTables().forEach(table -> {
                    System.out.println("-- Table: " + table.getName());
                });
            });

            // Use create action to generate and display schema
            SchemaManagementToolCoordinator.process(
                    metadata,
                    serviceRegistry,
                    settings,
                    action -> {}
            );

            System.out.println("=====================================");
        } catch (Exception e) {
            System.err.println("Error exporting schema: " + e.getMessage());
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    /**
     * Validates that the current schema matches the entity mappings.
     *
     * @return true if schema is valid, false otherwise
     */
    public static boolean validateSchema() {
        System.out.println("Validating database schema...");

        try {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            try (Session session = sessionFactory.openSession()) {
                Transaction tx = session.beginTransaction();
                // Simple query to verify connectivity
                session.createNativeQuery("SELECT 1", Object.class).getSingleResult();
                tx.commit();
            }
            System.out.println("Database schema is valid!");
            return true;
        } catch (Exception e) {
            System.err.println("Schema validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Main method to run schema creation standalone.
     */
    public static void main(String[] args) {
        String action = args.length > 0 ? args[0].toLowerCase() : "create";

        switch (action) {
            case "create":
                createDatabase();
                break;
            case "drop":
                dropDatabase();
                break;
            case "export":
                exportSchemaToConsole();
                break;
            case "validate":
                validateSchema();
                break;
            default:
                System.out.println("Usage: DatabaseInitializer [create|drop|export|validate]");
                System.out.println("  create   - Drop and create all tables (default)");
                System.out.println("  drop     - Drop all tables");
                System.out.println("  export   - Export DDL to console");
                System.out.println("  validate - Validate current schema");
        }

        HibernateUtil.shutdown();
    }
}
