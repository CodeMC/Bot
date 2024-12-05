package io.codemc.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("Test /codemc jenkins")
    public void testJenkins() {
        Jenkins jenkins = (Jenkins) command.getChildren()[0];

        assertEquals("jenkins", jenkins.getName());
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
        assertEquals(1, remove.getOptions().size());
        assertFalse(remove.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(remove);

        JenkinsAPI.createJenkinsUser("TestRemove", "1234");
        NexusAPI.createNexus("TestRemove", "1234");

        assertFalse(JenkinsAPI.getJenkinsUser("TestRemove").isEmpty());
        assertNotNull(NexusAPI.getNexusUser("TestRemove"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestRemove"), CommandUtil.embedSuccess("Successfully removed TestRemove from the CodeMC Services!"));

        assertTrue(JenkinsAPI.getJenkinsUser("TestRemove").isEmpty());
        assertNull(NexusAPI.getNexusUser("TestRemove"));
    }

    @Test
    @DisplayName("Test /codemc validate")
    public void testValidate() {
        Validate validate = (Validate) command.getChildren()[3];

        assertEquals("validate", validate.getName());
        assertEquals(1, validate.getOptions().size());
        assertFalse(validate.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(validate);

        // Test One - Jenkins Only

        JenkinsAPI.createJenkinsUser("TestValidate_1", "1234");

        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_1").isEmpty());
        assertNull(NexusAPI.getNexusUser("TestValidate_1"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestValidate_1"), CommandUtil.embedSuccess("Successfully validated 1 User(s)"));

        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_1").isEmpty());
        assertNotNull(NexusAPI.getNexusUser("TestValidate_1"));

        MockCodeMCBot.INSTANCE.delete("TestValidate_1");

        // Test One - Nexus Only

        NexusAPI.createNexus("TestValidate_2", "1234");

        assertTrue(JenkinsAPI.getJenkinsUser("TestValidate_2").isEmpty());
        assertNotNull(NexusAPI.getNexusUser("TestValidate_2"));

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestValidate_2"), CommandUtil.embedSuccess("Successfully validated 1 User(s)"));

        assertNotNull(JenkinsAPI.getJenkinsUser("TestValidate_2"));
        assertNotNull(NexusAPI.getNexusUser("TestValidate_2"));

        MockCodeMCBot.INSTANCE.delete("TestValidate_2");

        // Test All - Jenkins Only

        JenkinsAPI.createJenkinsUser("TestValidate_30", "1234");
        JenkinsAPI.createJenkinsUser("TestValidate_31", "1234");
        JenkinsAPI.createJenkinsUser("TestValidate_32", "1234");

        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_30").isEmpty());
        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_31").isEmpty());
        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_32").isEmpty());
        assertNull(NexusAPI.getNexusUser("TestValidate_30"));
        assertNull(NexusAPI.getNexusUser("TestValidate_31"));
        assertNull(NexusAPI.getNexusUser("TestValidate_32"));

        int size1 = JenkinsAPI.getAllJenkinsUsers().size();
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedSuccess("Successfully validated " + size1 + " User(s)"));

        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_30").isEmpty());
        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_31").isEmpty());
        assertFalse(JenkinsAPI.getJenkinsUser("TestValidate_32").isEmpty());
        assertNotNull(NexusAPI.getNexusUser("TestValidate_30"));
        assertNotNull(NexusAPI.getNexusUser("TestValidate_31"));
        assertNotNull(NexusAPI.getNexusUser("TestValidate_32"));

        MockCodeMCBot.INSTANCE.delete("TestValidate_30");
        MockCodeMCBot.INSTANCE.delete("TestValidate_31");
        MockCodeMCBot.INSTANCE.delete("TestValidate_32");
    }

    @Test
    @DisplayName("Test /codemc link")
    public void testLink() {
        Link link = (Link) command.getChildren()[4];

        assertEquals("link", link.getName());
        assertEquals(2, link.getOptions().size());
        assertFalse(link.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(link);

        Member m1 = MockJDA.mockMember("TestLink");
        MockJDA.GUILD.addRoleToMember(m1, MockJDA.AUTHOR);

        MockCodeMCBot.INSTANCE.create("TestLink", "Job");
        DatabaseAPI.removeUser("TestLink");

        assertNull(DatabaseAPI.getUser("TestLink"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink", "discord", m1), CommandUtil.embedSuccess("Linked Discord User TestLink to Jenkins User TestLink!"));
        assertNotNull(DatabaseAPI.getUser("TestLink"));
        assertEquals(m1.getIdLong(), DatabaseAPI.getUser("TestLink").getDiscord());

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink", "discord", m1), CommandUtil.embedSuccess("Linked Discord User TestLink to Jenkins User TestLink!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Jenkins User provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "Inexistent", "discord", m1), CommandUtil.embedError("The user does not have a Jenkins account!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink"), CommandUtil.embedError("Invalid Discord User provided!"));

        MockJDA.GUILD.removeRoleFromMember(m1, MockJDA.AUTHOR);

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestLink", "discord", m1), CommandUtil.embedError("The user is not an Author!"));
    
        MockCodeMCBot.INSTANCE.delete("TestLink");
        DatabaseAPI.removeUser("TestLink");
    }

    @Test
    @DisplayName("Test /codemc unlink")
    public void testUnlink() {
        Unlink unlink = (Unlink) command.getChildren()[5];

        assertEquals("unlink", unlink.getName());
        assertEquals(2, unlink.getOptions().size());
        assertFalse(unlink.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(unlink);

        Member m1 = MockJDA.mockMember("TestUnlink");
        MockJDA.GUILD.addRoleToMember(m1, MockJDA.AUTHOR);

        DatabaseAPI.removeUser("TestUnlink");
        MockCodeMCBot.INSTANCE.delete("TestUnlink");
        MockCodeMCBot.INSTANCE.create("TestUnlink", "Job");
        DatabaseAPI.addUser("TestUnlink", m1.getIdLong());

        assertNotNull(DatabaseAPI.getUser("TestUnlink"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("discord", m1), CommandUtil.embedSuccess("Unlinked Discord User TestUnlink from their Jenkins/Nexus account!"));
        assertNull(DatabaseAPI.getUser("TestUnlink"));

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Discord User provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("discord", m1), CommandUtil.embedError("The user is not linked to any Jenkins/Nexus account!"));
    
        MockCodeMCBot.INSTANCE.delete("TestUnlink");
        DatabaseAPI.removeUser("TestUnlink");
    }

    @Test
    @DisplayName("Test /codemc change-password")
    public void testChangePassword() {
        ChangePassword changePassword = (ChangePassword) command.getChildren()[6];

        assertEquals("change-password", changePassword.getName());
        assertEquals(1, changePassword.getOptions().size());

        SlashCommandEvent event = MockJDA.mockSlashCommandEvent(MockJDA.REQUEST_CHANNEL, changePassword, Map.of());
        TestCommandListener listener = new TestCommandListener(changePassword);

        JenkinsAPI.createJenkinsUser("User", "1234");
        NexusAPI.createNexus("User", "1234");
        DatabaseAPI.addUser("User", event.getMember().getIdLong());

        MockJDA.assertSlashCommandEvent(event, listener, CommandUtil.embedSuccess("Successfully changed your password!"));

        JenkinsAPI.deleteUser("User");
        NexusAPI.deleteNexus("User");
        DatabaseAPI.removeUser("User");
    }

    @Test
    @DisplayName("Test /codemc createuser")
    public void testCreateUser() {
        CreateUser createUser = (CreateUser) command.getChildren()[7];

        assertEquals("createuser", createUser.getName());
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
        assertEquals(1, delUser.getOptions().size());
        assertFalse(delUser.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(delUser);

        Member m1 = MockJDA.mockMember("TestDelUser");

        MockCodeMCBot.INSTANCE.create("TestDelUser", "Job");
        DatabaseAPI.addUser("TestDelUser", m1.getIdLong());

        assertFalse(JenkinsAPI.getJenkinsUser("TestDelUser").isEmpty());
        assertNotNull(NexusAPI.getNexusUser("TestDelUser"));
        assertNotNull(DatabaseAPI.getUser("TestDelUser"));
        assertEquals(m1.getIdLong(), DatabaseAPI.getUser("TestDelUser").getDiscord());

        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "TestDelUser"), CommandUtil.embedSuccess("Successfully deleted user TestDelUser!"));

        assertTrue(JenkinsAPI.getJenkinsUser("TestDelUser").isEmpty());
        assertNull(NexusAPI.getNexusUser("TestDelUser"));
        assertNull(DatabaseAPI.getUser("TestDelUser"));

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Invalid Username provided!"));
        MockJDA.assertSlashCommandEvent(listener, Map.of("username", "Inexistent"), CommandUtil.embedError("The user does not exist!"));
    
        DatabaseAPI.removeUser("TestDelUser");
    }

}
