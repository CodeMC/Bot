package io.codemc.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class CommandUtil{
    
    public CommandUtil(){}
    
    public String getString(SlashCommandEvent event, String key){
        OptionMapping mapping = event.getOption(key);
        if(mapping == null)
            return null;
        
        return mapping.getAsString();
    }
    
    public void sendError(SlashCommandEvent event, String reason){
        MessageEmbed embed = getErrorEmbed(reason);
        
        event.replyEmbeds(embed).setEphemeral(true).queue();
    }
    
    public void sendError(InteractionHook hook, String reason){
        MessageEmbed embed = getErrorEmbed(reason);
        
        hook.editOriginalEmbeds(embed).queue();
    }
    
    private MessageEmbed getErrorEmbed(String reason){
        return new EmbedBuilder()
            .setColor(0xFF0000)
            .setDescription(
                "There was an error while trying to handle the command!\n" +
                    "If this error persists, report it to Andre_601#0601"
            ).addField(
                "Error:",
                reason,
                false
            ).build();
    }
}
