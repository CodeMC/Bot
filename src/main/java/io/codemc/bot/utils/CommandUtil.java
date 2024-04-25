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
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.LoggerFactory;

public class CommandUtil{
    
    private static final Logger LOG = (Logger)LoggerFactory.getLogger(CommandUtil.class);
    
    public CommandUtil(){}
    
    public static EmbedBuilder getEmbed(){
        return new EmbedBuilder().setColor(0x0172BA);
    }
    
    public static class EmbedReply {
        
        private final SlashCommandEvent commandEvent;
        private final ModalInteractionEvent modalEvent;
        private final InteractionHook hook;
        private final EmbedBuilder builder = new EmbedBuilder();
        
        private EmbedReply(SlashCommandEvent commandEvent){
            this.commandEvent = commandEvent;
            this.modalEvent = null;
            this.hook = null;
        }
        
        private EmbedReply(InteractionHook hook){
            this.commandEvent = null;
            this.modalEvent = null;
            this.hook = hook;
        }
        
        private EmbedReply(ModalInteractionEvent modalEvent){
            this.commandEvent = null;
            this.modalEvent = modalEvent;
            this.hook = null;
        }
        
        public static EmbedReply fromCommandEvent(SlashCommandEvent event){
            return new EmbedReply(event);
        }
        
        public static EmbedReply fromModalEvent(ModalInteractionEvent event){
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
                .addField("Warning:", String.join("\n", lines), false);
            return this;
        }
        
        public void send(){
            if(commandEvent != null){
                commandEvent.replyEmbeds(builder.build()).queue();
            }else
            if(modalEvent != null){
                modalEvent.replyEmbeds(builder.build()).queue();
            }else
            if(hook != null){
                hook.editOriginalEmbeds(builder.build()).queue();
            }else{
                LOG.error("Received EmbedReply class with neither SlashCommandEvent, ModalInteractionEvent nor InteractionHook set!");
            }
        }
    }
}
