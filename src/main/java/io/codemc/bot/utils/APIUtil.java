package io.codemc.bot.utils;

import io.codemc.api.Generator;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.JavaContinuation;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class APIUtil {

    public static final int PASSWORD_SIZE = 24;
    private static final Logger LOGGER = LoggerFactory.getLogger(APIUtil.class);

    public APIUtil() {}

    public static String newPassword() {
        return Generator.createPassword(PASSWORD_SIZE);
    }

    public static void createNexus(InteractionHook hook, String username, String password) {
        CompletableFuture<Boolean> nexus = new CompletableFuture<>();
        nexus.whenCompleteAsync((success, ex) -> {
            if(ex != null || (success != null && !success)){
                if (hook != null)
                    CommandUtil.EmbedReply.from(hook)
                            .error("Failed to create Nexus Repository!")
                            .send();

                LOGGER.error("Failed to create Nexus Repository for {}!", username, ex);
            }
        });
        NexusAPI.createNexus(username, password, JavaContinuation.create(nexus));
    }

    public static void createJenkinsJob(InteractionHook hook, String username, String password, String project, String repoLink) {
        CompletableFuture<Boolean> isFreestyle = new CompletableFuture<>();
        isFreestyle.whenCompleteAsync((freestyle, ex) -> {
            if (ex != null) {
                CommandUtil.EmbedReply.from(hook)
                        .error("Failed to determine if the project is freestyle!")
                        .send();

                LOGGER.error("Failed to determine if the project is freestyle for {}!", username, ex);
                return;
            }

            boolean userSuccess = JenkinsAPI.createJenkinsUser(username, password);
            if (!userSuccess) {
                CommandUtil.EmbedReply.from(hook)
                        .error("Failed to create Jenkins User for {}!", username)
                        .send();

                LOGGER.error("Failed to create Jenkins User for {}!", username);
                return;
            }

            boolean jobSuccess = JenkinsAPI.createJenkinsJob(username, project, repoLink, freestyle);
            if (!jobSuccess) {
                CommandUtil.EmbedReply.from(hook)
                        .error("Failed to create Jenkins Job '{}' for {}!", project, username)
                        .send();

                LOGGER.error("Failed to create Jenkins Job '{}' for {}!", project, username);
                return;
            }

            boolean triggerBuild = JenkinsAPI.triggerBuild(username, project);
            if (!triggerBuild) {
                CommandUtil.EmbedReply.from(hook)
                        .error("Failed to trigger Jenkins Build for {}!", username)
                        .send();

                LOGGER.error("Failed to trigger Jenkins Build for {}!", username);
            }
        });
        JenkinsAPI.isFreestyle(repoLink, JavaContinuation.create(isFreestyle));
    }

}