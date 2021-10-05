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

import ch.qos.logback.classic.Logger;
import com.jagrosh.jdautilities.command.SlashCommand;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CmdApplication extends SlashCommand{
    
    private static final Logger LOG = (Logger)LoggerFactory.getLogger(CmdApplication.class);
    
    public CmdApplication(CodeMCBot bot){
        
        this.name = "application";
        this.help = "Accept or deny applications.";
    
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{
            Constants.ADMINISTRATOR,
            Constants.MODERATOR
        };
        
        this.children = new SlashCommand[]{
            new Accept(bot),
            new Deny(bot)
        };
    }
    
    @Override
    protected void execute(SlashCommandEvent event){}
    
    private static void handleResponse(CodeMCBot bot, InteractionHook hook, String id, String reason){
        Guild guild = hook.getJDA().getGuildById(Constants.SERVER);
        if(guild == null){
            bot.getCommandUtil().sendError(hook, "Unable to retrieve CodeMC Server.");
            return;
        }
        
        TextChannel requestAccess = guild.getTextChannelById(Constants.REQUEST_ACCESS);
        if(requestAccess == null){
            bot.getCommandUtil().sendError(hook, "Unable to retrieve request-access TextChannel.");
            return;
        }
        
        requestAccess.retrieveMessageById(id).queue(message -> {
            List<MessageEmbed> embeds = new ArrayList<>(message.getEmbeds());
            if(embeds.isEmpty()){
                bot.getCommandUtil().sendError(hook, "Message does not have any embeds.");
                return;
            }
    
            MessageEmbed embed = embeds.get(0);
            
            if(embed.getFooter() == null || embed.getFields().isEmpty()){
                bot.getCommandUtil().sendError(hook, "Embed does not have a footer or any fields.");
                return;
            }
            
            String userId = embed.getFooter().getText();
            if(userId == null || userId.isEmpty()){
                bot.getCommandUtil().sendError(hook, "Embed Footer does not have any text.");
                return;
            }
            
            String userLink = null;
            String repoLink = null;
            for(MessageEmbed.Field field : embed.getFields()){
                if(field.getName() == null || field.getValue() == null)
                    continue;
                
                if(field.getName().equalsIgnoreCase("user/organisation:")){
                    userLink = field.getValue();
                }else
                if(field.getName().equalsIgnoreCase("repository:")){
                    repoLink = field.getValue();
                }
            }
            
            if(userLink == null || repoLink == null){
                bot.getCommandUtil().sendError(hook, "Embed fields did not had the right values.");
                return;
            }
            
            String channelId = reason == null ? Constants.ACCEPTED_REQUESTS : Constants.REJECTED_REQUESTS;
            TextChannel channel = guild.getTextChannelById(channelId);
            if(channel == null){
                bot.getCommandUtil().sendError(hook, "Unable to retrieve channel with id " + channelId);
                return;
            }
            
            channel.sendMessage(getMessage(userId, userLink, repoLink, reason)).queue(m -> {
                message.delete().queue(
                    null,
                    e -> LOG.warn("Unable to delete message in request-access!")
                );
    
                if(reason != null){
                    hook.editOriginal("Denied User's Access request.").queue();
                    return;
                }
                
                Role authorRole = guild.getRoleById(Constants.AUTHOR);
                if(authorRole == null){
                    bot.getCommandUtil().sendError(hook, "Unable to retrieve Author Role.");
                    return;
                }
                
                guild.addRoleToMember(userId, authorRole).reason("[Access Request] Request of user accepted.").queue(
                    v -> hook.editOriginal("Accepted User's Access request.").queue(),
                    new ErrorHandler()
                        .handle(
                            ErrorResponse.MISSING_PERMISSIONS,
                            e -> bot.getCommandUtil().sendError(hook, "I lack the `Manage Roles` permission.")
                        ).handle(
                            ErrorResponse.UNKNOWN_MEMBER,
                            e -> bot.getCommandUtil().sendError(hook, "The user doesn't seem to be part of this server.")
                        ).handle(
                            ErrorResponse.UNKNOWN_ROLE,
                            e -> bot.getCommandUtil().sendError(hook, "Could not get the Author Role.")
                        )
                );
            }, e -> bot.getCommandUtil().sendError(hook, "Unable to send message in channel."));
        }, e -> bot.getCommandUtil().sendError(hook, "Unable to retrieve original submission."));
    }
    
    private static Message getMessage(String userId, String userLink, String repoLink, String reason){
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(reason == null ? 0x00FF00 : 0xFF0000)
            .setDescription(reason == null ? Constants.ACCEPTED_MSG : Constants.REJECTED_MSG)
            .addField(
                "user/organisation:",
                userLink,
                true
            ).addField(
                "Repository:",
                repoLink,
                true
            );
        
        if(reason != null){
            embed.addField(
                "Reason:",
                reason,
                false
            );
        }
        
        return new MessageBuilder("<@" + userId + ">")
            .setEmbeds(embed.build())
            .build();
    }
    
    private static class Accept extends SlashCommand{
        
        private final CodeMCBot bot;
        
        public Accept(CodeMCBot bot){
            this.bot = bot;
            
            this.name = "accept";
            this.help = "Accept an application.";
    
            this.defaultEnabled = false;
            this.enabledRoles = new String[]{
                Constants.ADMINISTRATOR,
                Constants.MODERATOR
            };
            
            this.options = Collections.singletonList(
                new OptionData(OptionType.STRING, "id", "The message ID of the application.").setRequired(true)
            );
        }
    
        @Override
        protected void execute(SlashCommandEvent event){
            String messageId = bot.getCommandUtil().getString(event, "id");
            
            if(messageId == null){
                bot.getCommandUtil().sendError(event, "Message ID was null.");
                return;
            }
            
            event.deferReply(true).queue(
                hook -> handleResponse(bot, hook, messageId, null)
            );
        }
    }
    
    private static class Deny extends SlashCommand{
        
        private final CodeMCBot bot;
        
        public Deny(CodeMCBot bot){
            this.bot = bot;
            
            this.name = "deny";
            this.help = "Deny an application.";
            
            this.defaultEnabled = false;
            this.enabledRoles = new String[]{
                Constants.ADMINISTRATOR,
                Constants.MODERATOR
            };
            
            this.options = Arrays.asList(
                new OptionData(OptionType.STRING, "id", "The message ID of the application.").setRequired(true),
                new OptionData(OptionType.STRING, "reason", "The reason of the denial.").setRequired(true)
            );
        }
    
        @Override
        protected void execute(SlashCommandEvent event){
            String messageId = bot.getCommandUtil().getString(event, "id");
            String reason = bot.getCommandUtil().getString(event, "reason");
            
            if(messageId == null || reason == null){
                bot.getCommandUtil().sendError(event, "Message ID or Reason was null.");
                return;
            }
    
            event.deferReply(true).queue(
                hook -> handleResponse(bot, hook, messageId, reason)
            );
        }
    }
}
