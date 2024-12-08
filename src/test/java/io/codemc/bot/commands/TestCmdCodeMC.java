package io.codemc.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.commands.CmdCodeMC.ChangePassword;
import io.codemc.bot.commands.CmdCodeMC.CreateUser;
import io.codemc.bot.commands.CmdCodeMC.DeleteUser;
import io.codemc.bot.commands.CmdCodeMC.Jenkins;
import io.codemc.bot.commands.CmdCodeMC.Link;
import io.codemc.bot.commands.CmdCodeMC.Nexus;
import io.codemc.bot.commands.CmdCodeMC.Remove;
import io.codemc.bot.commands.CmdCodeMC.Unlink;
import io.codemc.bot.commands.CmdCodeMC.Validate;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Member;

public class TestCmdCodeMC {

    private static CmdCodeMC command;

    @BeforeAll
    public static void init() {
        command = new CmdCodeMC(MockCodeMCBot.INSTANCE);

        MockCodeMCBot.INSTANCE.create("CodeMC", "Bot");
        MockCodeMCBot.INSTANCE.create("CodeMC", "API");
    }

    @AfterAll
    public static void cleanup() {
        MockCodeMCBot.INSTANCE.delete("CodeMC");
    }

    @Test
    @DisplayName("Test /codemc")
    public void testCodemc() {
        assertEquals("codemc", command.getName());
        assertFalse(command.getHelp().isEmpty());
        assertTrue(command.getChildren().length > 1);
    }

    @Test
    @DisplayName("Test /codemc jenkins")
    public void testJenkins() {
        Jenkins jenkins = (Jenkins) command.getChildren()[0];

        assertEquals("jenkins", jenkins.getName());
        assertFalse(jenkins.getHelp().isEmpty());
        assertEquals(1, jenkins.getOptions().size());

        TestCommandListener listener = new TestCommandListener(jenkins);
        
        MockJDA.assertSlashCommandEvent(listener, Map.of("job", "CodeMC/Bot"), jenkins.createInfoEmbed("CodeMC/Bot"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("job", "CodeMC/API"), jenkins.createInfoEmbed("CodeMC/API"));
    
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Jenkins Job provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("job", ""), CommandUtil.embedError("Invalid Jenkins Job provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("job", "InvalidJobName"), CommandUtil.embedError("Invalid Jenkins Job provided!"));
        
        MockJDA.assertSlashCommandEvent(listener, Map.of("job", "Inexistent/Inexistent"), CommandUtil.embedError("Failed to fetch Jenkins Job Info!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("job", "CodeMC/Inexistent"), CommandUtil.embedError("Failed to fetch Jenkins Job Info!"));
    }

    @Test
    @DisplayName("Test /codemc nexus")
    public void testNexus() {
        Nexus nexus = (Nexus) command.getChildren()[1];

        assertEquals("nexus", nexus.getName());
        assertFalse(nexus.getHelp().isEmpty());
        assertEquals(1, nexus.getOptions().size());

        TestCommandListener listener = new TestCommandListener(nexus);
        
        MockJDA.assertSlashCommandEvent(listener, Map.of("user", "CodeMC"), nexus.createInfoEmbed("CodeMC"));
    
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Username provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("user", ""), CommandUtil.embedError("Invalid Username provided!"));
        
        MockJDA.assertSlashCommandEvent(listener, Map.of("user", "Inexistent"), CommandUtil.embedError("Failed to fetch Nexus Repository Info!"));
    }

    @Test
    @DisplayName("Test /codemc remove")
    public void testRemove() {
        Remove remove = (Remove) command.getChildren()[2];

        assertEquals("remove", remove.getName());
        assertFalse(remove.getHelp().isEmpty());
        assertEquals(1, remove.getOptions().size());
        assertFalse(remove.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(remove);

        JenkinsAPI.createJenkinsUser("TestRemove", "1234");
        NexusAPI.createNexus("TestRemove", "1234");

        assertTrue(JenkinsAPI.existsUser("TestRemove"));
        assertTrue(NexusAPI.exists("TestRemove"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestRemove"), CommandUtil.embedSuccess("Successfully removed TestRemove from the CodeMC Services!"));

        assertFalse(JenkinsAPI.existsUser("TestRemove"));
        assertFalse(NexusAPI.exists("TestRemove"));

        MockCodeMCBot.INSTANCE.delete("TestRemove2");
        DatabaseAPI.removeUser("TestRemove2");
        Member m1 = MockJDA.mockMember("TestRemove2");
        MockJDA.GUILD.addRoleToMember(m1, MockJDA.AUTHOR);

        JenkinsAPI.createJenkinsUser("TestRemove2", "5678");
        NexusAPI.createNexus("TestRemove2", "5678");
        DatabaseAPI.addUser("TestRemove2", m1.getIdLong());

        assertTrue(JenkinsAPI.existsUser("TestRemove2"));
        assertTrue(NexusAPI.exists("TestRemove2"));
        assertNotNull(DatabaseAPI.getUser("TestRemove2"));
        assertEquals(m1.getIdLong(), DatabaseAPI.getUser("TestRemove2").getDiscord());

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestRemove2"), CommandUtil.embedSuccess("Revoked Author Status from TestRemove2!"));

        assertFalse(JenkinsAPI.existsUser("TestRemove2"));
        assertFalse(NexusAPI.exists("TestRemove2"));
        assertNull(DatabaseAPI.getUser("TestRemove2"));
        
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Jenkins User provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "Inexistent"), CommandUtil.embedError("The user does not have a Jenkins account!"));
    
        Member m2 = MockJDA.mockMember("TestRemove3");

        JenkinsAPI.createJenkinsUser("TestRemove3", "1234");
        NexusAPI.createNexus("TestRemove3", "1234");
        DatabaseAPI.addUser("TestRemove3", m2.getIdLong());

        assertTrue(JenkinsAPI.existsUser("TestRemove3"));
        assertTrue(NexusAPI.exists("TestRemove3"));
        assertNotNull(DatabaseAPI.getUser("TestRemove3"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestRemove3"), CommandUtil.embedError("User was deleted, but is not an Author!"));

        assertFalse(JenkinsAPI.existsUser("TestRemove3"));
        assertFalse(NexusAPI.exists("TestRemove3"));
        assertNull(DatabaseAPI.getUser("TestRemove3"));

        JenkinsAPI.createJenkinsUser("TestRemove4", "1234");
        NexusAPI.createNexus("TestRemove4", "1234");
        DatabaseAPI.addUser("TestRemove4", -10L);

        assertTrue(JenkinsAPI.existsUser("TestRemove4"));
        assertTrue(NexusAPI.exists("TestRemove4"));
        assertNotNull(DatabaseAPI.getUser("TestRemove4"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestRemove4"), CommandUtil.embedSuccess("Successfully removed TestRemove4 from the CodeMC Services!"));

        assertFalse(JenkinsAPI.existsUser("TestRemove4"));
        assertFalse(NexusAPI.exists("TestRemove4"));
        assertNull(DatabaseAPI.getUser("TestRemove4"));
    }

    @Test
    @DisplayName("Test /codemc validate")
    public void testValidate() {
        Validate validate = (Validate) command.getChildren()[3];

        assertEquals("validate", validate.getName());
        assertFalse(validate.getHelp().isEmpty());
        assertEquals(1, validate.getOptions().size());
        assertFalse(validate.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(validate);

        // Test One - Jenkins Only

        JenkinsAPI.createJenkinsUser("TestValidate_1", "1234");

        assertTrue(JenkinsAPI.existsUser("TestValidate_1"));
        assertFalse(NexusAPI.exists("TestValidate_1"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestValidate_1"), CommandUtil.embedSuccess("Successfully validated 1 User(s)"));

        assertTrue(JenkinsAPI.existsUser("TestValidate_1"));
        assertTrue(NexusAPI.exists("TestValidate_1"));

        MockCodeMCBot.INSTANCE.delete("TestValidate_1");

        // Test One - Nexus Only

        NexusAPI.createNexus("TestValidate_2", "1234");

        assertFalse(JenkinsAPI.existsUser("TestValidate_2"));
        assertTrue(NexusAPI.exists("TestValidate_2"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestValidate_2"), CommandUtil.embedSuccess("Successfully validated 1 User(s)"));

        assertTrue(JenkinsAPI.existsUser("TestValidate_2"));
        assertTrue(NexusAPI.exists("TestValidate_2"));

        MockCodeMCBot.INSTANCE.delete("TestValidate_2");

        // Test All - Jenkins Only

        JenkinsAPI.createJenkinsUser("TestValidate_30", "1234");
        JenkinsAPI.createJenkinsUser("TestValidate_31", "1234");
        JenkinsAPI.createJenkinsUser("TestValidate_32", "1234");

        assertTrue(JenkinsAPI.existsUser("TestValidate_30"));
        assertTrue(JenkinsAPI.existsUser("TestValidate_31"));
        assertTrue(JenkinsAPI.existsUser("TestValidate_32"));
        assertFalse(NexusAPI.exists("TestValidate_30"));
        assertFalse(NexusAPI.exists("TestValidate_31"));
        assertFalse(NexusAPI.exists("TestValidate_32"));

        int size1 = JenkinsAPI.getAllJenkinsUsers().size();
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedSuccess("Successfully validated " + size1 + " User(s)"));

        assertTrue(JenkinsAPI.existsUser("TestValidate_30"));
        assertTrue(JenkinsAPI.existsUser("TestValidate_31"));
        assertTrue(JenkinsAPI.existsUser("TestValidate_32"));
        assertTrue(NexusAPI.exists("TestValidate_30"));
        assertTrue(NexusAPI.exists("TestValidate_31"));
        assertTrue(NexusAPI.exists("TestValidate_32"));

        MockCodeMCBot.INSTANCE.delete("TestValidate_30");
        MockCodeMCBot.INSTANCE.delete("TestValidate_31");
        MockCodeMCBot.INSTANCE.delete("TestValidate_32");
    }

    @Test
    @DisplayName("Test /codemc link")
    public void testLink() {
        Link link = (Link) command.getChildren()[4];

        assertEquals("link", link.getName());
        assertFalse(link.getHelp().isEmpty());
        assertEquals(2, link.getOptions().size());
        assertFalse(link.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(link);

        Member m1 = MockJDA.mockMember("TestLink");
        MockJDA.GUILD.addRoleToMember(m1, MockJDA.AUTHOR);

        MockCodeMCBot.INSTANCE.create("TestLink", "Job");

        assertNull(DatabaseAPI.getUser("TestLink"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink", "discord", m1), CommandUtil.embedSuccess("Linked Discord User TestLink to Jenkins User TestLink!"));
        assertNotNull(DatabaseAPI.getUser("TestLink"));
        assertEquals(m1.getIdLong(), DatabaseAPI.getUser("TestLink").getDiscord());

        Member m2 = MockJDA.mockMember("TestLink2");
        MockJDA.GUILD.addRoleToMember(m2, MockJDA.AUTHOR);

        MockCodeMCBot.INSTANCE.create("TestLink2", "Job");
        MockCodeMCBot.INSTANCE.create("TestLink3", "Job");

        assertNull(DatabaseAPI.getUser("TestLink2"));
        assertNull(DatabaseAPI.getUser("TestLink3"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink2", "discord", m2), CommandUtil.embedSuccess("Linked Discord User TestLink2 to Jenkins User TestLink2!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink3", "discord", m2), CommandUtil.embedSuccess("Linked Discord User TestLink2 to Jenkins User TestLink3!"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink", "discord", m1), CommandUtil.embedSuccess("Linked Discord User TestLink to Jenkins User TestLink!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Jenkins User provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "Inexistent", "discord", m1), CommandUtil.embedError("The user does not have a Jenkins account!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink"), CommandUtil.embedError("Invalid Discord User provided!"));

        MockJDA.GUILD.removeRoleFromMember(m1, MockJDA.AUTHOR);

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink", "discord", m1), CommandUtil.embedError("The user is not an Author!"));
    
        MockCodeMCBot.INSTANCE.delete("TestLink");
        MockCodeMCBot.INSTANCE.delete("TestLink2");
        MockCodeMCBot.INSTANCE.delete("TestLink3");
        DatabaseAPI.removeUser("TestLink");
        DatabaseAPI.removeUser("TestLink2");
        DatabaseAPI.removeUser("TestLink3");
    }

    @Test
    @DisplayName("Test /codemc unlink")
    public void testUnlink() {
        Unlink unlink = (Unlink) command.getChildren()[5];

        assertEquals("unlink", unlink.getName());
        assertFalse(unlink.getHelp().isEmpty());
        assertEquals(2, unlink.getOptions().size());
        assertFalse(unlink.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(unlink);

        Member m1 = MockJDA.mockMember("TestUnlink");
        MockJDA.GUILD.addRoleToMember(m1, MockJDA.AUTHOR);

        MockCodeMCBot.INSTANCE.create("TestUnlink", "Job");
        DatabaseAPI.addUser("TestUnlink", m1.getIdLong());

        assertNotNull(DatabaseAPI.getUser("TestUnlink"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("discord", m1), CommandUtil.embedSuccess("Unlinked Discord User TestUnlink from their Jenkins/Nexus account!"));
        assertNull(DatabaseAPI.getUser("TestUnlink"));

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Discord User provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("discord", m1), CommandUtil.embedError("The user is not linked to any Jenkins/Nexus account!"));
    
        MockCodeMCBot.INSTANCE.delete("TestUnlink");
        DatabaseAPI.removeUser("TestUnlink");

        Member m2 = MockJDA.mockMember("TestUnlink2");
        MockJDA.GUILD.addRoleToMember(m2, MockJDA.AUTHOR);

        DatabaseAPI.removeUser("TestUnlink2");
        DatabaseAPI.removeUser("TestUnlink3");
        MockCodeMCBot.INSTANCE.create("TestUnlink2", "Job");
        MockCodeMCBot.INSTANCE.create("TestUnlink3", "Job");
        DatabaseAPI.addUser("TestUnlink2", m2.getIdLong());
        DatabaseAPI.addUser("TestUnlink3", m2.getIdLong());

        assertNotNull(DatabaseAPI.getUser("TestUnlink2"));
        assertNotNull(DatabaseAPI.getUser("TestUnlink3"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("discord", m2, "username", "TestUnlink3"),CommandUtil.embedSuccess("Unlinked Discord User TestUnlink2 from their Jenkins/Nexus account!"));
        assertNotNull(DatabaseAPI.getUser("TestUnlink2"));
        assertNull(DatabaseAPI.getUser("TestUnlink3"));

        MockCodeMCBot.INSTANCE.delete("TestUnlink2");
        MockCodeMCBot.INSTANCE.delete("TestUnlink3");
        DatabaseAPI.removeUser("TestUnlink2");
        DatabaseAPI.removeUser("TestUnlink3");
    }

    @Test
    @DisplayName("Test /codemc change-password")
    public void testChangePassword() {
        ChangePassword changePassword = (ChangePassword) command.getChildren()[6];

        assertEquals("change-password", changePassword.getName());
        assertFalse(changePassword.getHelp().isEmpty());
        assertEquals(1, changePassword.getOptions().size());

        SlashCommandEvent event = MockJDA.mockSlashCommandEvent(MockJDA.REQUEST_CHANNEL, changePassword, Map.of());
        TestCommandListener listener = new TestCommandListener(changePassword);

        JenkinsAPI.createJenkinsUser("Bot", "1234");
        NexusAPI.createNexus("Bot", "1234");
        DatabaseAPI.addUser("Bot", event.getMember().getIdLong());

        MockJDA.assertSlashCommandEvent(event, listener, CommandUtil.embedSuccess("Successfully changed your password!"));

        assertTrue(JenkinsAPI.deleteUser("Bot"));
        assertTrue(NexusAPI.deleteNexus("Bot"));

        MockJDA.assertSlashCommandEvent(event, listener, CommandUtil.embedError("You do not have a Jenkins account!"));

        assertEquals(1, DatabaseAPI.removeUser("Bot"));

        MockJDA.assertSlashCommandEvent(event, listener, CommandUtil.embedError("You are not linked to any Jenkins/Nexus accounts!"));

        when(event.getMember().getRoles()).thenReturn(List.of());

        MockJDA.assertSlashCommandEvent(event, listener, CommandUtil.embedError("Only Authors can regenerate their credentials."));
    }

    @Test
    @DisplayName("Test /codemc createuser")
    public void testCreateUser() {
        CreateUser createUser = (CreateUser) command.getChildren()[7];

        assertEquals("createuser", createUser.getName());
        assertFalse(createUser.getHelp().isEmpty());
        assertEquals(2, createUser.getOptions().size());
        assertFalse(createUser.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(createUser);

        Member m1 = MockJDA.mockMember("TestCreateUser");

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestCreateUser", "discord", m1), CommandUtil.embedSuccess("Successfully created user TestCreateUser and linked it to " + m1.getUser().getEffectiveName() + "!"));

        Member m2 = MockJDA.mockMember("TestCreateUser2");
        MockCodeMCBot.INSTANCE.create("TestCreateUser2", "Job");
        DatabaseAPI.addUser("TestCreateUser2", m2.getIdLong());

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestCreateUser2", "discord", m2), CommandUtil.embedError("A user with that username already exists."));
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Username provided!"));
        
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestCreateUser3"), CommandUtil.embedError("Invalid Discord User provided!"));

        MockCodeMCBot.INSTANCE.delete("TestCreateUser");
        MockCodeMCBot.INSTANCE.delete("TestCreateUser2");
        DatabaseAPI.removeUser("TestCreateUser");
        DatabaseAPI.removeUser("TestCreateUser2");
    }

    @Test
    @DisplayName("Test /codemc deluser")
    public void testDelUser() {
        DeleteUser delUser = (DeleteUser) command.getChildren()[8];

        assertEquals("deluser", delUser.getName());
        assertFalse(delUser.getHelp().isEmpty());
        assertEquals(1, delUser.getOptions().size());
        assertFalse(delUser.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(delUser);

        Member m1 = MockJDA.mockMember("TestDelUser");

        MockCodeMCBot.INSTANCE.create("TestDelUser", "Job");
        DatabaseAPI.addUser("TestDelUser", m1.getIdLong());

        assertTrue(JenkinsAPI.existsUser("TestDelUser"));
        assertTrue(NexusAPI.exists("TestDelUser"));
        assertNotNull(DatabaseAPI.getUser("TestDelUser"));
        assertEquals(m1.getIdLong(), DatabaseAPI.getUser("TestDelUser").getDiscord());

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestDelUser"), CommandUtil.embedSuccess("Successfully deleted user TestDelUser!"));

        assertFalse(JenkinsAPI.existsUser("TestDelUser"));
        assertFalse(NexusAPI.exists("TestDelUser"));
        assertNull(DatabaseAPI.getUser("TestDelUser"));

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Username provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "Inexistent"), CommandUtil.embedError("The user does not exist!"));
    
        DatabaseAPI.removeUser("TestDelUser");
    }

}
