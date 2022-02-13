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
import io.codemc.bot.utils.CommandUtil;
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
import okhttp3.HttpUrl;
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
    
    private static void handleResponse(CodeMCBot bot, InteractionHook hook, String id, String str, boolean accepted){
        Guild guild = hook.getJDA().getGuildById(Constants.SERVER);
        if(guild == null){
            CommandUtil.EmbedReply.fromHook(hook)
                .withError("Unable to retrieve CodeMC Sevrer.")
                .send();
            return;
        }
        
        TextChannel requestAccess = guild.getTextChannelById(Constants.REQUEST_ACCESS);
        if(requestAccess == null){
            CommandUtil.EmbedReply.fromHook(hook)
                .withError("Could not get the #request-access channel from the Server.")
                .send();
            return;
        }
        
        String projectUrl = getProjectUrlString(str);
        if(projectUrl == null && accepted){
            CommandUtil.EmbedReply.fromHook(hook)
                .withError("The provided Project URL is not a valid URL format.")
                .send();
            return;
        }
        
        requestAccess.retrieveMessageById(id).queue(message -> {
            List<MessageEmbed> embeds = new ArrayList<>(message.getEmbeds());
            if(embeds.isEmpty()){
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError("The retrieved message does not have any Embeds attached.")
                    .send();
                return;
            }
    
            MessageEmbed embed = embeds.get(0);
            
            if(embed.getFooter() == null || embed.getFields().isEmpty()){
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError("The Embed does not have a footer or any Embed Fields set.")
                    .send();
                return;
            }
            
            String userId = embed.getFooter().getText();
            if(userId == null || userId.isEmpty()){
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError("The Embed does not have a valid footer.")
                    .send();
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
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError("The Embed does not have valid Embed fields.")
                    .send();
                return;
            }
            
            String channelId = accepted ? Constants.ACCEPTED_REQUESTS : Constants.REJECTED_REQUESTS;
            TextChannel channel = guild.getTextChannelById(channelId);
            if(channel == null){
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError("Could not get " + (accepted ? "#accepted-requests" : "#rejected-requests") + " channel.")
                    .send();
                return;
            }
            
            channel.sendMessage(getMessage(userId, userLink, repoLink, accepted ? projectUrl : str, accepted)).queue(m -> {
                message.delete().queue(
                    null,
                    e -> LOG.warn("Unable to delete message in request-access!")
                );
    
                if(!accepted){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withMessage("Denied Application!")
                        .asSuccess()
                        .send();
                    return;
                }
                
                Role authorRole = guild.getRoleById(Constants.AUTHOR);
                if(authorRole == null){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withMessage("Accepted Application!")
                        .withWarning("Could not apply Author role to user. Role not found!")
                        .send();
                    return;
                }
                
                guild.addRoleToMember(userId, authorRole).reason("[Access Applications] Application of user accepted.").queue(
                    v -> CommandUtil.EmbedReply.fromHook(hook).withMessage("Accepted Application!").asSuccess().send(),
                    new ErrorHandler()
                        .handle(
                            ErrorResponse.MISSING_PERMISSIONS,
                            e -> CommandUtil.EmbedReply.fromHook(hook)
                                .withMessage("Accepted Application!")
                                .withWarning("I lack the `Manage Roles` permission to apply the Author role.")
                                .send()
                        ).handle(
                            ErrorResponse.UNKNOWN_MEMBER,
                            e -> CommandUtil.EmbedReply.fromHook(hook)
                                .withMessage("Accepted Application!")
                                .withWarning("Member was not found in the CodeMC Server.")
                                .send()
                        ).handle(
                            ErrorResponse.UNKNOWN_ROLE,
                            e -> CommandUtil.EmbedReply.fromHook(hook)
                                .withMessage("Accepted Application!")
                                .withWarning("The Author Role was not found.")
                                .send()
                        )
                );
            }, e -> CommandUtil.EmbedReply.fromHook(hook).withError("Cannot send message to channel.").send());
        }, e -> CommandUtil.EmbedReply.fromHook(hook).withError("Cannot retrieve message from #request-access.").send());
    }
    
    private static Message getMessage(String userId, String userLink, String repoLink, String str, boolean accepted){
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(accepted ? 0x00FF00 : 0xFF0000)
            .setDescription(accepted ? Constants.ACCEPTED_MSG : Constants.REJECTED_MSG)
            .addField(
                "user/organisation:",
                userLink,
                true
            ).addField(
                "Repository:",
                repoLink,
                true
            ).addField(
                accepted ? "New Project:" : "Reason:",
                str,
                false
            );
        
        return new MessageBuilder("<@" + userId + ">")
            .setEmbeds(embed.build())
            .build();
    }
    
    private static String getProjectUrlString(String url){
        HttpUrl projectUrl = HttpUrl.parse(url);
        
        if(projectUrl == null)
            return null;
        
        return String.format(
            "[`%s%s`](%s)",
            projectUrl.host(),
            projectUrl.encodedPath(),
            url
        );
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
            
            this.options = Arrays.asList(
                new OptionData(OptionType.STRING, "id", "The message ID of the application.").setRequired(true),
                new OptionData(OptionType.STRING, "project-url", "The Project URL to mention.").setRequired(true)
            );
        }
    
        @Override
        protected void execute(SlashCommandEvent event){
            String messageId = bot.getCommandUtil().getString(event, "id");
            String url = bot.getCommandUtil().getString(event, "project-url");
            
            if(messageId == null || url == null){
                CommandUtil.EmbedReply.fromEvent(event).withError("Message ID or Project URL was null.").send();
                return;
            }
            
            event.deferReply(true).queue(
                hook -> handleResponse(bot, hook, messageId, null, true)
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
                CommandUtil.EmbedReply.fromEvent(event).withError("Message ID or Reason was null.").send();
                return;
            }
    
            event.deferReply(true).queue(
                hook -> handleResponse(bot, hook, messageId, reason, false)
            );
        }
    }
}
