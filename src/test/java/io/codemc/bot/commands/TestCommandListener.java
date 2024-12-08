package io.codemc.bot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class TestCommandListener implements EventListener {

    private final BotCommand command;

    public TestCommandListener(SlashCommand command) {
        this.command = (BotCommand) command;
    }

    public BotCommand getCommand() {
        return command;
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof SlashCommandEvent commandEvent)
            command.execute(commandEvent);
    }

}