package io.codemc.bot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class CmdDisable extends SlashCommand{
    
    public CmdDisable(){
        this.name = "disable";
        this.help = "Disables the bot.";
        
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{
            Constants.ADMINISTRATOR,
            Constants.MODERATOR
        };
    }
    
    @Override
    protected void execute(SlashCommandEvent event){
        event.reply("Disabling bot...").setEphemeral(true).queue(m -> System.exit(0));
    }
}
