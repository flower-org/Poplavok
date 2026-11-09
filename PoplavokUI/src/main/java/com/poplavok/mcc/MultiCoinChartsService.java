package com.poplavok.mcc;

import com.multicoincharts.server.MultiCoinChartsServer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static com.flower.crypt.keys.UserPreferencesManager.getUserPreference;
import static com.flower.crypt.keys.UserPreferencesManager.updateUserPreference;

/**
 * Runs the embedded MultiCoinCharts web server for the UI.
 * All methods except {@link #shutdown()} must be called on the JavaFX thread; state changes are published there too.
 */
public class MultiCoinChartsService {
    final static Logger LOGGER = LoggerFactory.getLogger(MultiCoinChartsService.class);

    final static String PORT = "Poplavok_MccPort";
    final static String AUTO_START = "Poplavok_MccAutoStart";
    final static long SHUTDOWN_TIMEOUT_SECONDS = 10;

    public enum State { STOPPED, STARTING, RUNNING, STOPPING, FAILED }

    private static final MultiCoinChartsService INSTANCE = new MultiCoinChartsService();

    public static MultiCoinChartsService getInstance() {
        return INSTANCE;
    }

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.STOPPED);
    private final ReadOnlyStringWrapper statusText = new ReadOnlyStringWrapper("Stopped");
    @Nullable private MultiCoinChartsServer server;
    private int runningPort;
    // Bumped on every start/stop, so callbacks of a superseded start/stop don't overwrite the current state
    private int generation;

    private MultiCoinChartsService() {
    }

    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusTextProperty() {
        return statusText.getReadOnlyProperty();
    }

    public State getState() {
        return state.get();
    }

    public String url() {
        return "http://" + MultiCoinChartsServer.DEFAULT_HOST + ":" + runningPort + "/";
    }

    public static int savedPort() {
        int port = parsePort(getUserPreference(PORT));
        return port > 0 ? port : MultiCoinChartsServer.DEFAULT_PORT;
    }

    public static void savePort(int port) {
        updateUserPreference(Preferences.userRoot(), PORT, Integer.toString(port));
    }

    public static boolean isAutoStart() {
        return Boolean.parseBoolean(getUserPreference(AUTO_START));
    }

    public static void saveAutoStart(boolean autoStart) {
        updateUserPreference(Preferences.userRoot(), AUTO_START, Boolean.toString(autoStart));
    }

    /** @return port number, or -1 if the text is not a valid TCP port */
    public static int parsePort(@Nullable String text) {
        if (text == null || StringUtils.isBlank(text)) {
            return -1;
        }
        try {
            int port = Integer.parseInt(text.trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void start(int port) {
        if (server != null) {
            return;
        }
        MultiCoinChartsServer newServer = new MultiCoinChartsServer(MultiCoinChartsServer.DEFAULT_HOST, port);
        server = newServer;
        runningPort = port;
        int startGeneration = ++generation;
        setState(State.STARTING, "Starting on port " + port + "…");

        newServer.start().onComplete(result -> Platform.runLater(() -> {
            if (startGeneration != generation) {
                return;
            }
            if (result.succeeded()) {
                LOGGER.info("MultiCoinCharts server running at {}", url());
                setState(State.RUNNING, "Running at " + url());
            } else {
                server = null;
                setState(State.FAILED, "Failed to start on port " + port + ": " + describe(result.cause()));
            }
        }));
    }

    public void stop() {
        MultiCoinChartsServer current = server;
        if (current == null) {
            return;
        }
        server = null;
        int stopGeneration = ++generation;
        setState(State.STOPPING, "Stopping…");

        current.stop().onComplete(result -> Platform.runLater(() -> {
            if (stopGeneration != generation) {
                return;
            }
            setState(State.STOPPED, result.succeeded() ? "Stopped" : "Stopped with error: " + describe(result.cause()));
        }));
    }

    /** Stops the server on application exit, waiting for it to release the port. */
    public void shutdown() {
        MultiCoinChartsServer current = server;
        server = null;
        if (current == null) {
            return;
        }
        try {
            current.stop().toCompletionStage().toCompletableFuture().get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.warn("MultiCoinCharts server did not stop cleanly", e);
        }
    }

    private void setState(State newState, String text) {
        state.set(newState);
        statusText.set(text);
    }

    private static String describe(Throwable error) {
        return StringUtils.defaultIfBlank(error.getMessage(), error.getClass().getSimpleName());
    }
}
