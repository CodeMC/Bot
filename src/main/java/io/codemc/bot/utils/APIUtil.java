package io.codemc.bot.utils;

import io.codemc.api.Generator;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIUtil {

    public static final int PASSWORD_SIZE = 32;
    private static final Logger LOGGER = LoggerFactory.getLogger(APIUtil.class);

    public APIUtil() {}

    public static String newPassword() {
        return Generator.createPassword(PASSWORD_SIZE);
    }

    public static boolean createNexus(InteractionHook hook, String username, String password) {
        boolean success = NexusAPI.createNexus(username, password);
        if (!success) {
            if (hook != null)
                CommandUtil.EmbedReply.from(hook)
                        .error("Failed to create Nexus Repository!")
                        .send();

            LOGGER.error("Failed to create Nexus Repository for {}!", username);
            return false;
        }

        LOGGER.info("Successfully created Nexus Repository for {}!", username);
        return true;
    }

    public static boolean createJenkinsJob(InteractionHook hook, String username, String password, String project, String repoLink) {
        if (!JenkinsAPI.getJenkinsUser(username).isEmpty()) {
            CommandUtil.EmbedReply.from(hook)
                    .error("Jenkins User for " + username + " already exists!")
                    .send();

            LOGGER.error("Jenkins User for {} already exists!", username);
            return false;
        }

        boolean userSuccess = JenkinsAPI.createJenkinsUser(username, password);
        if (!userSuccess) {
            CommandUtil.EmbedReply.from(hook)
                    .error("Failed to create Jenkins User for " + username + "!")
                    .send();

            LOGGER.error("Failed to create Jenkins User for {}!", username);
            return false;
        }

        boolean freestyle = JenkinsAPI.isFreestyle(repoLink);
        boolean jobSuccess = JenkinsAPI.createJenkinsJob(username, project, repoLink, freestyle);
        if (!jobSuccess) {
            CommandUtil.EmbedReply.from(hook)
                    .error("Failed to create Jenkins Job '" + project + "' for " + username + "!")
                    .send();

            LOGGER.error("Failed to create Jenkins Job '{}' for {}!", project, username);
            return false;
        }

        boolean triggerBuild = JenkinsAPI.triggerBuild(username, project);
        if (!triggerBuild) {
            CommandUtil.EmbedReply.from(hook)
                    .error("Failed to trigger Jenkins Build for " + username + "!")
                    .send();

            LOGGER.error("Failed to trigger Jenkins Build for {}!", username);
            return false;
        }

        LOGGER.info("Successfully created Jenkins Job '{}' for {}!", project, username);
        return true;
    }

    public static boolean changePassword(InteractionHook hook, String username, String newPassword) {
        boolean jenkinsSuccess = JenkinsAPI.changeJenkinsPassword(username, newPassword);
        if (!jenkinsSuccess) {
            CommandUtil.EmbedReply.from(hook)
                    .error("Failed to change Jenkins Password for " + username + "!")
                    .send();

            LOGGER.error("Failed to change Jenkins Password for {}!", username);
            return false;
        }

        boolean nexusSuccess = NexusAPI.changeNexusPassword(username, newPassword);
        if (!nexusSuccess) {
            CommandUtil.EmbedReply.from(hook)
                    .error("Failed to change Nexus Password for " + username + "!")
                    .send();

            LOGGER.error("Failed to change Nexus Password for {}!", username);
            return false;
        }
        return true;
    }

}
