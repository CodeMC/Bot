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

package io.codemc.bot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.ApplicationHandler;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class CmdApplication extends BotCommand{

    public CmdApplication(CodeMCBot bot){
        super(bot);
        
        this.name = "application";
        this.help = "Accept or deny applications.";
        
        this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
        
        this.children = new SlashCommand[]{
            new Accept(bot),
            new Deny(bot)
        };
    }
    
    @Override
    public void withModalReply(SlashCommandEvent event){}
    
    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){}
    
    private static class Accept extends BotCommand{

        public Accept(CodeMCBot bot){
            super(bot);
            
            this.name = "accept";
            this.help = "Accept an application";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
            
            this.options = List.of(
                    new OptionData(OptionType.STRING, "id", "The message id of the application.").setRequired(true)
            );
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){}
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            String message = event.getOption("id", null, OptionMapping::getAsString);
            if (message == null) {
                CommandUtil.EmbedReply.from(hook).error("Message ID was not present!").send();
                return;
            }

            try {
                long messageId;
                if (message.contains("-"))
                    messageId = Long.parseLong(message.split("-")[1]);
                else
                    messageId = Long.parseLong(message);

                if(messageId == -1L){
                    CommandUtil.EmbedReply.from(hook).error("Message ID was not present!").send();
                    return;
                }
                
                ApplicationHandler.handle(bot, hook, guild, messageId, null, true);
            } catch (NumberFormatException e) {
                CommandUtil.EmbedReply.from(hook).error("Invalid message ID!").send();
            }
        }
    }
    
    private static class Deny extends BotCommand{
        
        public Deny(CodeMCBot bot){
            super(bot);
            
            this.name = "deny";
            this.help = "Deny an application";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
            
            this.options = List.of(
                    new OptionData(OptionType.STRING, "id", "The message id of the application.").setRequired(true),
                    new OptionData(OptionType.STRING, "reason", "The reason for the denial").setRequired(true)
            );
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){}
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            String message = event.getOption("id", null, OptionMapping::getAsString);
            if (message == null) {
                CommandUtil.EmbedReply.from(hook).error("Message ID was not present!").send();
                return;
            }

            try {
                long messageId;
                if (message.contains("-"))
                    messageId = Long.parseLong(message.split("-")[1]);
                else
                    messageId = Long.parseLong(message);
                
                String reason = event.getOption("reason", null, OptionMapping::getAsString);
                
                if(messageId == -1L || reason == null){
                    CommandUtil.EmbedReply.from(hook).error("Message ID or Reason were not present!").send();
                    return;
                }
                
                ApplicationHandler.handle(bot, hook, guild, messageId, reason, false);
            } catch (NumberFormatException e) {
                CommandUtil.EmbedReply.from(hook).error("Invalid message ID!").send();
            }
        }
    }
}
