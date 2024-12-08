package io.codemc.bot.utils;

import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
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
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, hook, GUILD, message.getIdLong(), null, true
        );

        String jenkinsUrl = MockCodeMCBot.INSTANCE.getConfigHandler().getString("jenkins", "url") + "/job/" + username + "/job/Job/";
        MessageCreateData expected = ApplicationHandler.getMessage(MockCodeMCBot.INSTANCE, member.getId(), "userLink", "repoLink", jenkinsUrl, SELF.getUser(), true);
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
    }

    @Test
    @DisplayName("Test ApplicationHandler#handle (Rejected)")
    public void testHandleRejected() {
        String username = "TestApplicationHandlerRejected";
        Member member = MockJDA.mockMember(username);
        InteractionHook hook = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, hook, GUILD, message.getIdLong(), "Denied", false
        );

        MessageCreateData expected = ApplicationHandler.getMessage(MockCodeMCBot.INSTANCE, member.getId(), "userLink", "repoLink", "Denied", SELF.getUser(), false);
        String content = MockJDA.getLatestMessage(REJECTED_CHANNEL);
        List<MessageEmbed> embeds = MockJDA.getLatestEmbeds(REJECTED_CHANNEL);

        assertEquals(expected.getContent(), content);
        assertEmbeds(expected.getEmbeds(), embeds, true);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));
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
            List.of(CommandUtil.embedError("Provided Message does not have any embeds.")), MockJDA.getEmbeds(h1.getIdLong()), true
        );

        InteractionHook h2 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        MessageEmbed e2 = CommandUtil.getEmbed().build();
        Message m2 = MockJDA.mockMessage("", List.of(e2), REQUEST_CHANNEL);

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h2, GUILD, m2.getIdLong(), null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Embed does not have a Footer or any Embed Fields")), MockJDA.getEmbeds(h2.getIdLong()), true
        );

        InteractionHook h3 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        MessageEmbed e3 = CommandUtil.getEmbed().setFooter(" ").addField("null", "null", true).build();
        Message m3 = MockJDA.mockMessage("", List.of(e3), REQUEST_CHANNEL);

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h3, GUILD, m3.getIdLong(), null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Embed does not have a valid footer.")), MockJDA.getEmbeds(h3.getIdLong()), true
        );

        InteractionHook h4 = MockJDA.mockInteractionHook(SELF, REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        MessageEmbed e4 = CommandUtil.getEmbed().setFooter("id").addField("null", "null", true).build();
        Message m4 = MockJDA.mockMessage("", List.of(e4), REQUEST_CHANNEL);

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, h4, GUILD, m4.getIdLong(), null, true
        );
        assertEmbeds(
            List.of(CommandUtil.embedError("Embed does not have all valid Fields.")), MockJDA.getEmbeds(h4.getIdLong()), true
        );
    }

}
