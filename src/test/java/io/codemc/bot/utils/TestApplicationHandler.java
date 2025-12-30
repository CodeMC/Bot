package io.codemc.bot.utils;

import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.database.Request;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.codemc.bot.MockJDA.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestApplicationHandler {
    
    @Test
    @DisplayName("Test ApplicationHandler#handle (Accepted)")
    public void testHandleAccepted() {
        String username = "TestApplicationHandlerAccepted";
        Member member = MockJDA.mockMember(username);
        InteractionHook hook = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description");
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);
        DatabaseAPI.createRequest(message.getIdLong(), member.getIdLong(), username, "Job");

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, hook, GUILD, message.getIdLong(), null, true
        );

        String jenkinsUrl = MockCodeMCBot.INSTANCE.getConfigHandler().getString("jenkins", "url") + "/job/" + username + "/job/Job/";
        MessageCreateData expected = ApplicationHandler.getMessage(MockCodeMCBot.INSTANCE, member.getId(), "https://github.com/TestApplicationHandlerAccepted", "https://github.com/TestApplicationHandlerAccepted/Job", jenkinsUrl, SELF.getUser(), true);
        String content = MockJDA.getLatestMessage(ACCEPTED_CHANNEL);
        List<MessageEmbed> embeds = MockJDA.getLatestEmbeds(ACCEPTED_CHANNEL);

        assertEquals(expected.getContent(), content);
        assertEmbeds(expected.getEmbeds(), embeds, true);

        assertTrue(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertTrue(JenkinsAPI.existsUser(username));
        assertTrue(NexusAPI.exists(username));
        assertNotNull(DatabaseAPI.getUser(username));
        assertEquals(member.getIdLong(), DatabaseAPI.getUser(username).getDiscord());

        assertTrue(JenkinsAPI.deleteUser(username));
        assertTrue(NexusAPI.deleteNexus(username));
        assertEquals(1, DatabaseAPI.removeUser(username));
        assertEquals(1, DatabaseAPI.removeRequest(message.getIdLong()));
    }

    @Test
    @DisplayName("Test ApplicationHandler#handle (Rejected)")
    public void testHandleRejected() {
        String username = "TestApplicationHandlerRejected";
        Member member = MockJDA.mockMember(username);
        InteractionHook hook = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description");
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);
        DatabaseAPI.createRequest(message.getIdLong(), member.getIdLong(), username, "Job");

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, hook, GUILD, message.getIdLong(), "Denied", false
        );
        
        String userLink = "https://github.com/" + username;
        String repoLink = "https://github.com/" + username + "/Job";
        MessageCreateData expected = ApplicationHandler.getMessage(MockCodeMCBot.INSTANCE, member.getId(), userLink, repoLink, "Denied", SELF.getUser(), false);
        String content = MockJDA.getLatestMessage(REJECTED_CHANNEL);
        List<MessageEmbed> embeds = MockJDA.getLatestEmbeds(REJECTED_CHANNEL);

        assertEquals(expected.getContent(), content);
        assertEmbeds(expected.getEmbeds(), embeds, true);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        assertEquals(1, DatabaseAPI.removeRequest(message.getIdLong()));
    }

    @Test
    @DisplayName("Test ApplicationHandler#handle (Errors)")
    public void testHandleErrors() {
        InteractionHook h1 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        Message m1 = MockJDA.mockMessage("", List.of(), REQUEST_CHANNEL);

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h1, GUILD, m1.getIdLong(), null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Request not found in Database and Message has no embeds to parse from.")), MockJDA.getEmbeds(h1.getIdLong()), true
        );

        InteractionHook h2 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        DatabaseAPI.createRequest(123, 0, "", "");

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h2, GUILD, 123, null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Request does not have a valid user.")), MockJDA.getEmbeds(h2.getIdLong()), true
        );
        DatabaseAPI.removeRequest(123);

        InteractionHook h3 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        DatabaseAPI.createRequest(1, 1, "user", "");

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h3, GUILD, 1, null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Database Request is missing values.")), MockJDA.getEmbeds(h3.getIdLong()), true
        );
        DatabaseAPI.removeRequest(1);

        InteractionHook h4 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        DatabaseAPI.createRequest(2, 2, "", "job");

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h4, GUILD, 2, null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Database Request is missing values.")), MockJDA.getEmbeds(h3.getIdLong()), true
        );
        DatabaseAPI.removeRequest(2);

        InteractionHook h5 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        Message m5 = MockJDA.mockMessage("", List.of(
                CommandUtil.requestEmbed(
                        ";;?invalidLink",
                        "''.invalidRepoLink",
                        "User <@1234567890123456789>",
                        "Description"
                )
        ), REQUEST_CHANNEL);

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h5, GUILD, m5.getIdLong(), null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Request not found in Database and data could not be parsed from embed.")), MockJDA.getEmbeds(h5.getIdLong()), true
        );
    }


    @Test
    @DisplayName("Test ApplicationHandler#fromMessage")
    public void testFromMessage() {
        long messageId = 1234567890123456789L;
        long userId = 555555555555555555L;
        String username = "TestApplicationHandlerFromMessage";
        String mention = "<@" + userId + ">";
        String repo = "Job";
        String description = "This is a test description.";

        String userLink = MarkdownUtil.maskedLink(username, "https://github.com/" + username);
        String repoLink = MarkdownUtil.maskedLink(repo, "https://github.com/" + username + "/" + repo);
        String submitter = String.format("`%s` (%s)", username, mention);

        MessageEmbed embed = CommandUtil.requestEmbed(userLink, repoLink, submitter, description);

        Request request = ApplicationHandler.fromEmbed(messageId, embed);
        assertNotNull(request);
        assertEquals(messageId, request.getMessageId());
        assertEquals(userId, request.getUserId());
        assertEquals(username, request.getGithubName());
        assertEquals(repo, request.getRepoName());
    }

}
