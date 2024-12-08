package io.codemc.bot.commands;

import io.codemc.bot.MockCodeMCBot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestCmdDisable {
    
    @Test
    @DisplayName("Test /disable")
    public void testDisable() {
        CmdDisable command = new CmdDisable(MockCodeMCBot.INSTANCE);

        assertEquals("disable", command.getName());
        assertFalse(command.getHelp().isEmpty());
        assertEquals(0, command.getOptions().size());
        assertFalse(command.allowedRoles.isEmpty());

        // Command calls System.exit - cannot be tested
    }

}
