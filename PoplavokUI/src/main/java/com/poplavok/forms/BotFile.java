package com.poplavok.forms;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Saves a running bot: every change goes to {@code <file>.bak}; on exit (stop, finish, tab close) the bot file is
 * overwritten and the backup deleted. So a backup found when loading means the bot didn't shut down cleanly.
 */
final class BotFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotFile.class);

    final File file;
    /** Shows a message to the user; called on the FX thread. */
    private final Consumer<String> messages;
    /** Bot's latest state, so saving never takes the bot's lock (the bot holds it when calling us). */
    private SellDownBotState latest;
    /** True if {@code latest} isn't written to the bot file yet. */
    private boolean unsaved;

    BotFile(File file, SellDownBotState state, Consumer<String> messages) {
        this.file = file;
        this.latest = state;
        this.messages = messages;
    }

    static File backupFile(File file) {
        return new File(file.getPath() + ".bak");
    }

    /** Called by the bot, holding its lock, on every change. */
    synchronized void changed(SellDownBotState state) {
        latest = state;
        unsaved = true;
        write(state, backupFile(file));
    }

    /** Overwrites the bot file with the bot's state (if it changed), then deletes the backup. */
    synchronized void saveOnExit() {
        if (unsaved && write(latest, file)) {
            unsaved = false;
        }
        File backup = backupFile(file);
        if (!unsaved && backup.exists() && !backup.delete()) {
            LOGGER.error("Failed to delete {}", backup);
            Platform.runLater(() -> messages.accept("Couldn't delete " + backup.getName()));
        }
    }

    private boolean write(SellDownBotState state, File target) {
        try {
            state.write(target);
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save sell-down bot to {}", target, e);
            Platform.runLater(() -> messages.accept("Save to " + target.getName() + " failed: " + e.getMessage()));
            return false;
        }
    }
}
