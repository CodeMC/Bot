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

import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.commands.CmdCodeMC.Jenkins;
import io.codemc.bot.commands.CmdCodeMC.Nexus;
import io.codemc.bot.commands.CmdCodeMC.Remove;
import io.codemc.bot.commands.CmdCodeMC.Validate;
import io.codemc.bot.utils.APIUtil;
import io.codemc.bot.utils.CommandUtil;

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

        MockCodeMCBot.INSTANCE.delete( "TestValidate_30");
        MockCodeMCBot.INSTANCE.delete("TestValidate_31");
        MockCodeMCBot.INSTANCE.delete("TestValidate_32");
    }

}
