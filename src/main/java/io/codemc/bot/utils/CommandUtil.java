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

import ch.qos.logback.classic.Logger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.LoggerFactory;

public class CommandUtil{
    
    private static final Logger LOG = (Logger)LoggerFactory.getLogger(CommandUtil.class);
    
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
    
    public boolean lackPerms(SlashCommandEvent event, Guild guild, TextChannel tc, Permission... permissions){
        for(Permission permission : permissions){
            if(!guild.getSelfMember().hasPermission(tc, permission)){
                EmbedReply.fromEvent(event)
                    .withError(String.format(
                        "I lack the `%s` permission for %s",
                        permission.getName(),
                        tc.getAsMention()
                    ))
                    .send();
                return true;
            }
        }
        
        return false;
    }
    
    public EmbedBuilder getEmbed(){
        return new EmbedBuilder().setColor(0x0172BA);
    }
    
    public static class EmbedReply {
        
        private final InteractionHook hook;
        private final SlashCommandEvent event;
        private final EmbedBuilder builder = new EmbedBuilder();
        
        private EmbedReply(SlashCommandEvent event){
            this.event = event;
            this.hook = null;
        }
        
        private EmbedReply(InteractionHook hook){
            this.event = null;
            this.hook = hook;
        }
        
        public static EmbedReply fromEvent(SlashCommandEvent event){
            return new EmbedReply(event);
        }
        
        public static EmbedReply fromHook(InteractionHook hook){
            return new EmbedReply(hook);
        }
        
        public EmbedReply withMessage(String... lines){
            builder.setDescription(String.join("\n", lines));
            return this;
        }
        
        public EmbedReply asSuccess(){
            builder.setColor(0x00FF00);
            return this;
        }
        
        public EmbedReply withError(String... lines){
            builder.setColor(0xFF0000)
                .setDescription(
                    "There was an error while trying to handle the command!\n" +
                    "If this error persists, report it to Andre_601#0601"
                )
                .addField("Error:", String.join("\n", lines), false);
            return this;
        }
        
        public EmbedReply withIssue(String... lines){
            builder.setColor(0xFFC800)
                .setDescription(
                    "There was an issue while handling the command.\n" +
                    "If this issue persists, report it to Andre_601#0601"
                )
                .addField("Warning:", String.join("\n", lines), false);
            return this;
        }
        
        public void send(){
            if(event != null){
                event.replyEmbeds(builder.build()).queue();
            }else
            if(hook != null){
                hook.editOriginalEmbeds(builder.build()).queue();
            }else{
                LOG.error("Received EmbedReply class with neither SlashCommandEvent nor InteractionHook set!");
            }
        }
    }
}
