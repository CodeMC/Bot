package io.codemc.bot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;

public class TestCmdSubmit {

    private static CmdSubmit command;

    @BeforeAll
    public static void init() {
        command = new CmdSubmit(MockCodeMCBot.INSTANCE);
    }
    
    @Test
    @DisplayName("Test /submit")
    public void testSubmit() {
        assertEquals("submit", command.getName());
        assertEquals(0, command.getOptions().size());
        assertTrue(command.hasModalReply);

        TestCommandListener listener = new TestCommandListener(command);

        MockJDA.assertSlashCommandEvent(listener, null);
        assertNotNull(MockJDA.CURRENT_MODAL);
        assertEquals("submit", MockJDA.CURRENT_MODAL.getId());
        assertEquals(4, MockJDA.CURRENT_MODAL.getComponents().size());
    }

}
