package io.codemc.bot.commands;

import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.commands.CmdMsg.Edit;
import io.codemc.bot.commands.CmdMsg.Post;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.codemc.bot.MockJDA.GENERAL;
import static org.junit.jupiter.api.Assertions.*;

public class TestCmdMsg {

    private static CmdMsg command;

    @BeforeAll
    public static void setup() {
        command = new CmdMsg(MockCodeMCBot.INSTANCE);
    }

    @Test
    @DisplayName("Test /msg")
    public void testMsg() {
        assertEquals("msg", command.getName());
        assertFalse(command.getHelp().isEmpty());
        assertTrue(command.getChildren().length > 1);
    }

    @Test
    @DisplayName("Test /msg post")
    public void testPost() {
        Post post = new Post(MockCodeMCBot.INSTANCE);

        assertEquals("send", post.getName());
        assertFalse(post.getHelp().isEmpty());
        assertFalse(post.allowedRoles.isEmpty());
        assertTrue(post.hasModalReply);
        assertEquals(2, post.getOptions().size());

        TestCommandListener listener = new TestCommandListener(post);

        MockJDA.assertSlashCommandEvent(listener, Map.of("channel", GENERAL, "embed", true), (MessageEmbed[]) null);
        MockJDA.assertSlashCommandEvent(listener, Map.of("channel", GENERAL, "embed", false), (MessageEmbed[]) null);

        assertNotNull(MockJDA.CURRENT_MODAL);

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Received invalid Channel input."));
    }

    @Test
    @DisplayName("Test /msg edit")
    public void testEdit() {
        Edit edit = new Edit(MockCodeMCBot.INSTANCE);

        assertEquals("edit", edit.getName());
        assertFalse(edit.getHelp().isEmpty());
        assertFalse(edit.allowedRoles.isEmpty());
        assertTrue(edit.hasModalReply);
        assertEquals(3, edit.getOptions().size());

        TestCommandListener listener = new TestCommandListener(edit);
        Message message = MockJDA.mockMessage("", GENERAL);

        MockJDA.assertSlashCommandEvent(listener, Map.of("channel", GENERAL, "id", message.getIdLong(), "embed", true), (MessageEmbed[]) null);
        MockJDA.assertSlashCommandEvent(listener, Map.of("channel", GENERAL, "id", message.getIdLong(), "embed", false), (MessageEmbed[]) null);

        assertNotNull(MockJDA.CURRENT_MODAL);

        MockJDA.assertSlashCommandEvent(listener, Map.of(), CommandUtil.embedError("Received invalid Channel or Message ID."));
        MockJDA.assertSlashCommandEvent(listener, Map.of("channel", GENERAL), CommandUtil.embedError("Received invalid Channel or Message ID."));
        MockJDA.assertSlashCommandEvent(listener, Map.of("id", message.getIdLong()), CommandUtil.embedError("Received invalid Channel or Message ID."));
    }

}
