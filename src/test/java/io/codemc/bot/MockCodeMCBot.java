package io.codemc.bot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.LoggerFactory;

import io.codemc.bot.config.ConfigHandler;

public class MockCodeMCBot extends CodeMCBot {

    public static MockCodeMCBot INSTANCE = new MockCodeMCBot();

    private MockCodeMCBot() {
        logger = LoggerFactory.getLogger(MockCodeMCBot.class);
        configHandler = new ConfigHandler();
        configHandler.loadConfig();

        // Set Nexus Password
        try {
            File file = new File("/tmp/admin.password");
            if (file.exists()) {
                String password = Files.readString(file.toPath());
                configHandler.set(password, "nexus", "password");
            }
        } catch (IOException e) {
            logger.error("Failed to read Nexus password from file", e);
        }

        // Set GitHub Token
        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isEmpty()) {
            configHandler.set(System.getenv("GITHUB_TOKEN"), "github");
        }

        start();
    }

    @Override
    void start() {
        logger.info("Starting test bot...");

        validateConfig();
        initializeAPI();
    }
}
