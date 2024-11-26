package io.codemc.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.commands.CmdCodeMC.Jenkins;
import io.codemc.bot.commands.CmdCodeMC.Nexus;
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

}
