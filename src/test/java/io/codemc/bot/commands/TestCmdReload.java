package io.codemc.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.utils.CommandUtil;

public class TestCmdReload {

    @Test
    @DisplayName("Test /reload")
    public void testReload() {
        CmdReload command = new CmdReload(MockCodeMCBot.INSTANCE);

        assertEquals("reload", command.getName());
        assertFalse(command.getHelp().isEmpty());
        assertEquals(0, command.getOptions().size());
        assertFalse(command.allowedRoles.isEmpty());

        TestCommandListener listener = new TestCommandListener(command);

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedSuccess("Reload success!"));

        MockCodeMCBot.INSTANCE.setTestConfig();
    }

}