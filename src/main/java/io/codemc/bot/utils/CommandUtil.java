/*
 * Copyright 2021 CodeMC.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.codemc.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
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
    
    public boolean getBoolean(SlashCommandEvent event, String key, boolean def){
        OptionMapping mapping = event.getOption(key);
        if(mapping == null)
            return def;
        
        return mapping.getAsBoolean();
    }
    
    public TextChannel getChannel(SlashCommandEvent event, String key){
        OptionMapping mapping = event.getOption(key);
        if(mapping == null)
            return null;
        
        return (TextChannel)mapping.getAsGuildChannel();
    }
    
    public void sendError(SlashCommandEvent event, String... reason){
        MessageEmbed embed = getErrorEmbed(String.join("\n", reason));
        
        event.replyEmbeds(embed).setEphemeral(true).queue();
    }
    
    public void sendError(InteractionHook hook, String... reason){
        MessageEmbed embed = getErrorEmbed(String.join("\n", reason));
        
        hook.editOriginalEmbeds(embed).queue();
    }
    
    public EmbedBuilder getEmbed(){
        return new EmbedBuilder().setColor(0x0172BA);
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
