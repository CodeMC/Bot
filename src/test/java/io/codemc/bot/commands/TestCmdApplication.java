package io.codemc.bot.commands;

import static io.codemc.bot.MockJDA.AUTHOR;
import static io.codemc.bot.MockJDA.REQUEST_CHANNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.commands.CmdApplication.Accept;
import io.codemc.bot.commands.CmdApplication.Deny;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class TestCmdApplication {
    
    private static CmdApplication command;

    @BeforeAll
    public static void init() {
        command = new CmdApplication(MockCodeMCBot.INSTANCE);
    }

    @Test
    @DisplayName("Test /application")
    public void testApplication() {
        assertEquals("application", command.getName());
        assertFalse(command.getHelp().isEmpty());
        assertEquals(0, command.getOptions().size());
        assertFalse(command.allowedRoles.isEmpty());
        assertTrue(command.getChildren().length > 0);
    }

    @Test
    @DisplayName("Test /application accept")
    public void testAccept() {
        Accept accept = (Accept) command.getChildren()[0];

        assertEquals("accept", accept.getName());
        assertFalse(accept.getHelp().isEmpty());
        assertEquals(1, accept.getOptions().size());
        assertFalse(accept.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(accept);
        String username = "TestApplicationAccept";
        Member member = MockJDA.mockMember(username);
        
        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        long id = MockJDA.assertSlashCommandEvent(listener, Map.of("id", message.getId()), (MessageEmbed[]) null);

        String expected = String.format("""
            [5/5] Handling Join Request...
            - [<:like:935126958193405962>] Message retrieved!
            - [<:like:935126958193405962>] Message validated!
              - Embed found!
              - Found User ID `%s`.
              - User and Repository Link found and validated!
            - [<:like:935126958193405962>] `accepted-requests` channel found!
            - [<:like:935126958193405962>] Join Request removed!
              - Thread archived!
              - Request Message deleted!
            - [<:like:935126958193405962>] Gave User Role!
              - Found Author Role!
              - Applied Author Role to User!
            
            **Successfully accepted Join Request of user %s!**
            """, member.getId(), member.getUser().getEffectiveName());

        assertEquals(expected, MockJDA.getMessage(id));

        assertTrue(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertTrue(JenkinsAPI.existsUser(username));
        assertTrue(NexusAPI.exists(username));
        assertNotNull(DatabaseAPI.getUser(username));
        assertEquals(member.getIdLong(), DatabaseAPI.getUser(username).getDiscord());

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Message ID was not present!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("id", "abcd"), CommandUtil.embedError("Invalid message ID!"));

        assertTrue(JenkinsAPI.deleteUser(username));
        assertTrue(NexusAPI.deleteNexus(username));
        assertEquals(1, DatabaseAPI.removeUser(username));
    }

    @Test
    @DisplayName("Test /application deny")
    public void testDeny() {
        Deny deny = (Deny) command.getChildren()[1];

        assertEquals("deny", deny.getName());
        assertFalse(deny.getHelp().isEmpty());
        assertEquals(2, deny.getOptions().size());
        assertFalse(deny.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(deny);

        String username = "TestApplicationDeny";
        Member member = MockJDA.mockMember(username);

        MessageEmbed embed = CommandUtil.requestEmbed("[" + username + "](userLink)", "[Job](repoLink)", member.getAsMention(), "description", member.getId());
        Message message = MockJDA.mockMessage("", List.of(embed), REQUEST_CHANNEL);

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));

        long id = MockJDA.assertSlashCommandEvent(listener, Map.of("id", message.getId(), "reason", "Denied"), (MessageEmbed[]) null);

        String expected = String.format(                        """
            [<:like:935126958193405962>] Handling of Join Request complete!
            - [<:like:935126958193405962>] Message retrieved!
            - [<:like:935126958193405962>] Message validated!
              - Embed found!
              - Found User ID `%s`.
              - User and Repository Link found and validated!
            - [<:like:935126958193405962>] `rejected-requests` channel found!
            - [<:like:935126958193405962>] Join Request removed!
              - Thread archived!
              - Request Message deleted!
            - [<:like:935126958193405962>] Finished rejecting join request of %s!
            """, member.getId(), member.getUser().getEffectiveName());

        assertEquals(expected, MockJDA.getMessage(id));

        assertFalse(CommandUtil.hasRole(member, List.of(AUTHOR.getIdLong())));
        assertFalse(JenkinsAPI.existsUser(username));
        assertFalse(NexusAPI.exists(username));
        assertNull(DatabaseAPI.getUser(username));
    
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Message ID was not present!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("id", "abcd"), CommandUtil.embedError("Invalid message ID!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("id", "0"), CommandUtil.embedError("Message ID or Reason were not present!"));
    }

}
