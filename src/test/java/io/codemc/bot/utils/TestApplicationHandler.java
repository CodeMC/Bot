package io.codemc.bot.utils;

import static io.codemc.bot.MockJDA.ACCEPTED_CHANNEL;
import static io.codemc.bot.MockJDA.AUTHOR;
import static io.codemc.bot.MockJDA.GUILD;
import static io.codemc.bot.MockJDA.REJECTED_CHANNEL;
import static io.codemc.bot.MockJDA.REQUEST_CHANNEL;
import static io.codemc.bot.MockJDA.SELF;
import static io.codemc.bot.MockJDA.assertEmbeds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertTrue(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNull(NexusAPI.getNexusUser(username));
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
        assertFalse(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNotNull(NexusAPI.getNexusUser(username));
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
        assertTrue(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNull(NexusAPI.getNexusUser(username));
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
        assertTrue(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNull(NexusAPI.getNexusUser(username));
        assertNull(DatabaseAPI.getUser(username));
    }

}
