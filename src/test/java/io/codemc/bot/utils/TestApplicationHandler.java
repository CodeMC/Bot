package io.codemc.bot.utils;

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

public class TestApplicationHandler {
    
    @Test
    @DisplayName("Test ApplicationHandler#handle (Accepted)")
    public void testHandleAccepted() {
        String username = "TestApplicationHandlerAccepted";
        Member member = MockJDA.mockMember(username);
        InteractionHook hook = MockJDA.mockInteractionHook(member, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), MockJDA.REQUEST_CHANNEL);

        assertFalse(CommandUtil.hasRole(member, List.of(MockJDA.AUTHOR.getIdLong())));
        assertTrue(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNull(NexusAPI.getNexusUser(username));
        assertNull(DatabaseAPI.getUser(username));

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, hook, MockJDA.GUILD, message.getIdLong(), null, true
        );

        assertTrue(CommandUtil.hasRole(member, List.of(MockJDA.AUTHOR.getIdLong())));
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
        InteractionHook hook = MockJDA.mockInteractionHook(member, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), MockJDA.REQUEST_CHANNEL);

        assertFalse(CommandUtil.hasRole(member, List.of(MockJDA.AUTHOR.getIdLong())));
        assertTrue(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNull(NexusAPI.getNexusUser(username));
        assertNull(DatabaseAPI.getUser(username));

        ApplicationHandler.handle(
            MockCodeMCBot.INSTANCE, hook, MockJDA.GUILD, message.getIdLong(), "Denied", false
        );

        assertFalse(CommandUtil.hasRole(member, List.of(MockJDA.AUTHOR.getIdLong())));
        assertTrue(JenkinsAPI.getJenkinsUser(username).isEmpty());
        assertNull(NexusAPI.getNexusUser(username));
        assertNull(DatabaseAPI.getUser(username));
    }

}
