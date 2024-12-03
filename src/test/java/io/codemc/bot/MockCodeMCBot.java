package io.codemc.bot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.LoggerFactory;

import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.config.ConfigHandler;
import io.codemc.bot.utils.APIUtil;

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

    public void create(String username, String job) {
        String link = "https://github.com/" + username + "/" + job;
        if (JenkinsAPI.getJenkinsUser(username).isEmpty()) {
            String password = APIUtil.newPassword();
            APIUtil.createJenkinsJob(null, username, password, job, link, false);
            APIUtil.createNexus(null, username, password);
        } else {
            JenkinsAPI.createJenkinsJob(username, job, link, JenkinsAPI.isFreestyle(link));
        }
    }

    public void delete(String username) {
        JenkinsAPI.deleteUser(username);
        NexusAPI.deleteNexus(username);
    }
}
