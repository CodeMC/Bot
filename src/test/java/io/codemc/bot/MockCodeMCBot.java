package io.codemc.bot;

import org.slf4j.LoggerFactory;

import io.codemc.bot.config.MockConfigHandler;

public class MockCodeMCBot extends CodeMCBot {

    public static MockCodeMCBot INSTANCE;

    MockCodeMCBot() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Cannot create multiple instances of TestCodeMCBot");
        }

        INSTANCE = this;
        logger = LoggerFactory.getLogger(MockCodeMCBot.class);
        configHandler = new MockConfigHandler();
    }

    @Override
    void start() {
        logger.info("Starting test bot...");

        validateConfig();
        initializeAPI();
    }
}
