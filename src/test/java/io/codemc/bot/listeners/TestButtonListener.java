package io.codemc.bot.listeners;

import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.codemc.bot.MockJDA.AUTHOR;
import static io.codemc.bot.MockJDA.REQUEST_CHANNEL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestButtonListener {
    
    private static ButtonListener listener;

    @BeforeAll
    public static void init() {
        listener = new ButtonListener(MockCodeMCBot.INSTANCE);
    }

    @Test
    @DisplayName("Test Application Accept")
    public void testApplicationAccept() {
        String username = "TestButtonListenerAccept";
        Member member = MockJDA.mockMember(username);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);

        JenkinsAPI.deleteUser(username);
        NexusAPI.deleteNexus(username);
        DatabaseAPI.removeUser(username);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        MockJDA.assertButtonInteractionEvent(listener, message, Button.success("application:accept:" + username + ":Job", "Accept"), (MessageEmbed[]) null);

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
    @DisplayName("Test Application Deny")
    public void testApplicationDeny() {
        String username = "TestButtonListenerDeny";
        Member member = MockJDA.mockMember(username);

        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);

        JenkinsAPI.deleteUser(username);
        NexusAPI.deleteNexus(username);
        DatabaseAPI.removeUser(username);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));

        MockJDA.assertButtonInteractionEvent(listener, message, Button.danger("application:deny:" + username + ":Job", "Deny"), (MessageEmbed[]) null);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
    }

    @Test
    @DisplayName("Test ButtonListener Errors")
    public void testButtonListenerErrors() {
        Message m1 = MockJDA.mockMessage(null, REQUEST_CHANNEL);
        ButtonInteractionEvent e1 = MockJDA.mockButtonInteractionEvent(m1, Button.primary("null", "null"));
        when(e1.getGuild()).thenReturn(null);
        MockJDA.assertButtonInteractionEvent(listener, e1, CommandUtil.embedError("Buttons only work on the CodeMC Server!"));

        Message m2 = MockJDA.mockMessage(null, REQUEST_CHANNEL);
        Button b2 = mock(Button.class);
        when(b2.getId()).thenReturn(null);
        ButtonInteractionEvent e2 = MockJDA.mockButtonInteractionEvent(m2, b2);
        MockJDA.assertButtonInteractionEvent(listener, e2, CommandUtil.embedError("Received Button Interaction with no ID!"));

        Message m3 = MockJDA.mockMessage(null, REQUEST_CHANNEL);
        ButtonInteractionEvent e3 = MockJDA.mockButtonInteractionEvent(m3, Button.primary("null", "null"));
        when(e3.getMember()).thenReturn(null);
        MockJDA.assertButtonInteractionEvent(listener, e3, CommandUtil.embedError("Cannot get Member from Server!"));

        Message m4 = MockJDA.mockMessage(null, REQUEST_CHANNEL);
        ButtonInteractionEvent e4 = MockJDA.mockButtonInteractionEvent(m4, Button.primary("null", "null"));
        MockJDA.assertButtonInteractionEvent(listener, e4, CommandUtil.embedError("Received non-application button event!"));

        Message m5 = MockJDA.mockMessage(null, REQUEST_CHANNEL);
        ButtonInteractionEvent e5 = MockJDA.mockButtonInteractionEvent(m5, Button.primary("application:null:null:null", "null"));
        MockJDA.assertButtonInteractionEvent(listener, e5, CommandUtil.embedError("Received unknown Button Application type.", "Expected `accept` or `deny` but got `null`."));
    }

    @Test
    @DisplayName("Test ButtonListener#lacksRole")
    public void testLacksRole() {
        assertTrue(listener.lacksRole(List.of(), List.of()));

        assertTrue(listener.lacksRole(List.of(1L, 2L, 3L), List.of(4L)));
        assertTrue(listener.lacksRole(List.of(1L, 2L, 3L), List.of(4L, 5L)));
        assertTrue(listener.lacksRole(List.of(2L, 3L, 4L), List.of(5L, 6L, 7L)));

        assertFalse(listener.lacksRole(List.of(1L, 2L, 3L), List.of(1L)));
        assertFalse(listener.lacksRole(List.of(1L, 2L, 3L), List.of(1L, 2L)));
    }

}
