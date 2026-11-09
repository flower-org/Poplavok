package com.multicoincharts.server;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/** Standalone entry point. See {@link #USAGE} for command line options. */
public class MultiCoinChartsMain {
    // Must run before the first logger is created; an explicit -Dlog4j2.configurationFile still wins.
    static {
        if (System.getProperty("log4j2.configurationFile") == null) {
            System.setProperty("log4j2.configurationFile", "log4j2-standalone.xml");
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiCoinChartsMain.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage: MultiCoinChartsServer [--host HOST] [--port PORT]",
            "  --host HOST  address to listen on (default " + MultiCoinChartsServer.DEFAULT_HOST
                    + "; 0.0.0.0 for all interfaces)",
            "  --port PORT  port to listen on (default " + MultiCoinChartsServer.DEFAULT_PORT + ")",
            "  --help       show this help");

    public static void main(String[] args) throws Exception {
        String host = MultiCoinChartsServer.DEFAULT_HOST;
        int port = MultiCoinChartsServer.DEFAULT_PORT;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--host" -> host = value(args, ++i, "--host");
                    case "--port" -> port = parsePort(value(args, ++i, "--port"));
                    case "-h", "--help" -> {
                        System.out.println(USAGE);
                        return;
                    }
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
        }

        MultiCoinChartsServer server = new MultiCoinChartsServer(host, port);

        try {
            server.start().toCompletionStage().toCompletableFuture().get();
        } catch (Exception e) {
            LOGGER.error("MultiCoinCharts server failed to start", e);
            LogManager.shutdown();
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop().toCompletionStage().toCompletableFuture().get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.error("MultiCoinCharts server did not stop cleanly", e);
            } finally {
                LogManager.shutdown();
            }
        }, "mcc-shutdown"));
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port >= 0 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        throw new IllegalArgumentException("Invalid port: " + value);
    }
}
