package io.codemc.bot.commands;

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBotCommand {

    private static MockBotCommand command;

    private static final class MockBotCommand extends BotCommand {

        MockBotCommand() {
            super(MockCodeMCBot.INSTANCE);
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {}

        @Override
        public void withModalReply(SlashCommandEvent event) {}
        
    }

    @BeforeAll
    public static void init() {
        command = new MockBotCommand();
    }

    @Test
    @DisplayName("Test BotCommand Errors")
    public void testErrors() {
        TestCommandListener listener = new TestCommandListener(command);

        SlashCommandEvent e1 = MockJDA.mockSlashCommandEvent(MockJDA.GENERAL, command, Map.of());
        when(e1.getGuild()).thenReturn(null);
        MockJDA.assertSlashCommandEvent(e1, listener, CommandUtil.embedError("Command can only be executed in a Server!"));

        SlashCommandEvent e2 = MockJDA.mockSlashCommandEvent(MockJDA.GENERAL, command, Map.of());
        when(e2.getGuild()).thenAnswer(inv -> {
            Guild g = mock(Guild.class);
            when(g.getIdLong()).thenReturn(123L);
            return g;
        });
        MockJDA.assertSlashCommandEvent(e2, listener, CommandUtil.embedError("Unable to find CodeMC Server!"));
        
        SlashCommandEvent e3 = MockJDA.mockSlashCommandEvent(MockJDA.GENERAL, command, Map.of());
        when(e3.getMember()).thenReturn(null);

        MockJDA.assertSlashCommandEvent(e3, listener, CommandUtil.embedError("Unable to retrieve Member from Event!"));
    }

}
