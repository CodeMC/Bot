package io.codemc.bot.listeners;

import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.codemc.bot.MockJDA.GENERAL;
import static io.codemc.bot.MockJDA.REQUEST_CHANNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class TestModalListener {

    private static ModalListener listener;

    @BeforeAll
    public static void init() {
        listener = new ModalListener(MockCodeMCBot.INSTANCE);
    }
    
    @Test
    @DisplayName("Test Submit")
    public void testSubmit() {
        String username = "TestModalListenerSubmit";
        Modal modal = MockJDA.mockModal("submit", "Submit");
        Map<String, String> options = Map.of(
            "user", username,
            "repo", "Job",
            "description", "description",
            "repoLink", "repoLink"
        );

        MockJDA.assertModalInteractionEvent(listener, modal, GENERAL, options, CommandUtil.embedSuccess("[Request sent!](<Jump URL>)"));
        MockJDA.assertModalInteractionEvent(listener, modal, GENERAL, Map.of("user", username, "repo", "Job", "description", "description"), CommandUtil.embedSuccess("[Request sent!](<Jump URL>)"));

        MockJDA.assertModalInteractionEvent(listener, modal, GENERAL, Map.of(), CommandUtil.embedError("The provided user was invalid."));

        JenkinsAPI.createJenkinsUser(username, "1234");
        MockJDA.assertModalInteractionEvent(listener, modal, GENERAL, options, CommandUtil.embedError("A Jenkins User named '" + username + "' already exists!"));
        JenkinsAPI.deleteUser(username);

        MockJDA.assertModalInteractionEvent(
            listener, modal, GENERAL, Map.of("user", username), 
            CommandUtil.embedError("The option User, Repo and/or Description was not set properly!")
        );
        MockJDA.assertModalInteractionEvent(
            listener, modal, GENERAL, Map.of("user", username, "description", "Description"), 
            CommandUtil.embedError("The option User, Repo and/or Description was not set properly!")
        );
        MockJDA.assertModalInteractionEvent(
            listener, modal, GENERAL, Map.of("user", username, "repo", "Job"), 
            CommandUtil.embedError("The option User, Repo and/or Description was not set properly!")
        );
        MockJDA.assertModalInteractionEvent(
            listener, modal, GENERAL, Map.of("user", username, "repo", "", "description", "Description"), 
            CommandUtil.embedError("The option User, Repo and/or Description was not set properly!")
        );
        MockJDA.assertModalInteractionEvent(
            listener, modal, GENERAL, Map.of("user", username, "repo", "Job", "description", ""), 
            CommandUtil.embedError("The option User, Repo and/or Description was not set properly!")
        );
    }

    @Test
    @DisplayName("Test Deny Application")
    public void testDenyApplication() {
        String username = "TestModalApplicationDeny";
        Member member = MockJDA.mockMember(username);

        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description");

        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);
        Message message2 = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);
        DatabaseAPI.createRequest(message.getIdLong(), member.getIdLong(), username, "Job");
        DatabaseAPI.createRequest(message2.getIdLong(), member.getIdLong(), username, "Job");

        Modal m1 = MockJDA.mockModal("deny_application:" + message.getId(), "Deny Application");
        Modal m2 = MockJDA.mockModal("deny_application:" + message2.getId(), "Deny Application");

        long id1 = MockJDA.assertModalInteractionEvent(listener, m1, REQUEST_CHANNEL, Map.of(), (MessageEmbed[]) null);
        long id2 = MockJDA.assertModalInteractionEvent(listener, m2, REQUEST_CHANNEL, Map.of("reason", "reason"), (MessageEmbed[]) null);

        String expected = String.format("""
            [<:like:935126958193405962>] Handling of Join Request complete!
            - [<:like:935126958193405962>] Request retrieved!
                - Found User ID `%s`.
                - User and Repository found and validated!
            - [<:like:935126958193405962>] `rejected-requests` channel found!
            - [<:like:935126958193405962>] Join Request removed!
                - Thread archived!
                - Request Message deleted!
            - [<:like:935126958193405962>] Finished rejecting join request of %s!
            """, member.getId(), member.getUser().getEffectiveName());

        assertEquals(expected, MockJDA.getMessage(id1));
        assertEquals(expected, MockJDA.getMessage(id2));

        Modal m3 = MockJDA.mockModal("deny_application", "Deny Application");
        MockJDA.assertModalInteractionEvent(listener, m3, REQUEST_CHANNEL, Map.of(), CommandUtil.embedError("Received invalid Deny Application modal!"));

        Modal m4 = MockJDA.mockModal("deny_application:abcd", "Deny Application");
        MockJDA.assertModalInteractionEvent(listener, m4, REQUEST_CHANNEL, Map.of(), CommandUtil.embedError("Received invalid message ID: abcd"));
    
        assertEquals(1, DatabaseAPI.removeRequest(message.getIdLong()));
        assertEquals(1, DatabaseAPI.removeRequest(message2.getIdLong()));
    }

    @Test
    @DisplayName("Test Message")
    public void testMessage() {
        String channelId = REQUEST_CHANNEL.getId();
        
        // Test Post
        Modal m1 = MockJDA.mockModal("message:post:" + channelId + ":true", "Message");
        MockJDA.assertModalInteractionEvent(listener, m1, GENERAL, Map.of("message", "test"), CommandUtil.embedSuccess("[Message sent!](<Jump URL>)"));

        Modal m2 = MockJDA.mockModal("message:post:" + channelId + ":false", "Message");
        MockJDA.assertModalInteractionEvent(listener, m2, GENERAL, Map.of("message", "test"), CommandUtil.embedSuccess("[Message sent!](<Jump URL>)"));

        // Test Edit
        Message msg1 = MockJDA.mockMessage("Message", GENERAL);
        Modal m3 = MockJDA.mockModal("message:edit:" + channelId + ":true:" + msg1.getId(), "Message");
        MockJDA.assertModalInteractionEvent(listener, m3, GENERAL, Map.of("message", "test"), CommandUtil.embedSuccess("[Message edited!](<Jump URL>)"));

        Message msg2 = MockJDA.mockMessage("Message", GENERAL);
        Modal m4 = MockJDA.mockModal("message:edit:" + channelId + ":false:" + msg2.getId(), "Message");
        MockJDA.assertModalInteractionEvent(listener, m4, GENERAL, Map.of("message", "test"), CommandUtil.embedSuccess("[Message edited!](<Jump URL>)"));

        // Test Errors
        Modal m5 = MockJDA.mockModal("message", "Message");
        MockJDA.assertModalInteractionEvent(listener, m5, GENERAL, Map.of(), CommandUtil.embedError(("Invalid Modal data. Expected `4+` arguments but received `1`!")));

        Modal m6 = MockJDA.mockModal("message:post:abcd:true", "Message");
        MockJDA.assertModalInteractionEvent(listener, m6, GENERAL, Map.of(), CommandUtil.embedError("Received invalid Text Channel."));

        Modal m7 = MockJDA.mockModal("message:post:" + channelId + ":true", "Message");
        MockJDA.assertModalInteractionEvent(listener, m7, GENERAL, Map.of(), CommandUtil.embedError("Received invalid Message to sent/edit."));

        Modal m8 = MockJDA.mockModal("message:edit:" + channelId + ":true", "Message");
        MockJDA.assertModalInteractionEvent(listener, m8, GENERAL, Map.of("message", "test"), CommandUtil.embedError("Received invalid Modal data. Expected `>4` but got `=4`"));

        Modal m9 = MockJDA.mockModal("message:edit:" + channelId + ":true:abcd", "Message");
        MockJDA.assertModalInteractionEvent(listener, m9, GENERAL, Map.of("message", "test"), CommandUtil.embedError("Received invalid message ID `abcd`."));

        Modal m10 = MockJDA.mockModal("message:unknown:" + channelId + ":true", "Message");
        MockJDA.assertModalInteractionEvent(listener, m10, GENERAL, Map.of("message", "test"), CommandUtil.embedError("Received Unknown Message type: `unknown`."));
    }

    @Test
    @DisplayName("Test ModalListener Errors")
    public void testModalListenerErrors() {
        Modal modal = MockJDA.mockModal("null", "null");

        ModalInteractionEvent e1 = MockJDA.mockModalInteractionEvent(modal, REQUEST_CHANNEL, Map.of());
        when(e1.getGuild()).thenReturn(null);
        MockJDA.assertModalInteractionEvent(listener, e1, CommandUtil.embedError("Unable to retrieve CodeMC Server!"));

        ModalInteractionEvent e2 = MockJDA.mockModalInteractionEvent(modal, REQUEST_CHANNEL, Map.of());
        MockJDA.assertModalInteractionEvent(listener, e2, CommandUtil.embedError("Received Modal with unknown ID `null`."));
    }

}
