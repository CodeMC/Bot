/*
 * Copyright 2024 CodeMC.io
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

package io.codemc.bot.listeners;

import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.ApplicationHandler;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModalListener extends ListenerAdapter{
    
    private final Logger logger = LoggerFactory.getLogger(ModalListener.class);
    
    private final CodeMCBot bot;
    
    public ModalListener(CodeMCBot bot){
        this.bot = bot;
    }
    
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event){
        if(!event.isFromGuild())
            return;
        
        Guild guild = event.getGuild();
        if(guild == null || guild.getIdLong() != bot.getConfigHandler().getLong("server")){
            CommandUtil.EmbedReply.from(event).error("Unable to retrieve CodeMC Server!").send();
            return;
        }
        
        String[] args = event.getModalId().split(":");
        
        switch(args[0]){
            case "submit" -> event.deferReply(true).queue(hook -> {
                String user = value(event, "user");

                if(user == null || user.isEmpty()){
                    CommandUtil.EmbedReply.from(hook).error("The provided user was invalid.").send();
                    return;
                }
                
                if (!JenkinsAPI.getJenkinsUser(user).isEmpty()) {
                    CommandUtil.EmbedReply.from(hook)
                            .error("A Jenkins User named '" + user + "' already exists!")
                            .send();
                    return;
                }

                String repo = value(event, "repo");
                String description = value(event, "description");
                
                if(repo == null || repo.isEmpty() || description == null || description.isEmpty()){
                    CommandUtil.EmbedReply.from(hook).error(
                        "The option User, Repo and/or Description was not set properly!")
                        .send();
                    return;
                }
                
                TextChannel requestChannel = guild.getTextChannelById(bot.getConfigHandler().getLong("channels", "request_access"));
                if(requestChannel == null){
                    CommandUtil.EmbedReply.from(hook).error("Unable to retrieve `request-access` channel!").send();                     
                    return;
                }
                
                String repoLinkValue = value(event, "repoLink");
                if(repoLinkValue == null || repoLinkValue.isEmpty())
                    repoLinkValue = "https://github.com/" + user + "/" + repo;
                
                String userLink = MarkdownUtil.maskedLink(user, "https://github.com/" + user);
                String repoLink = MarkdownUtil.maskedLink(repo, repoLinkValue);
                String submitter = String.format("`%s` (%s)", event.getUser().getEffectiveName(), event.getUser().getAsMention());
                
                MessageEmbed embed = CommandUtil.requestEmbed(userLink, repoLink, submitter, description, event.getUser().getId());
                
                requestChannel.sendMessageEmbeds(embed)
                    .setActionRow(
                        Button.success("application:accept:" + user + ":" + repo, "Accept"),
                        Button.danger("application:deny:" + user + ":" + repo, "Deny")
                    ).queue(
                        message -> {
                            CommandUtil.EmbedReply.from(hook).success(
                                "[Request sent!](" + message.getJumpUrl() + ")")
                                .send();
                            
                            message.createThreadChannel("Access Request - " + event.getUser().getName()).queue();
                            message.addReaction(Emoji.fromCustom("like", 935126958193405962L, false)).queue();
                            message.addReaction(Emoji.fromCustom("dislike", 935126958235344927L, false)).queue();
                            
                            logger.info("[Access Request] User {} requested access to the CI.", event.getUser().getEffectiveName());
                        },
                        e -> {
                            CommandUtil.EmbedReply.from(hook).error(
                                "Error while submitting request!",
                                "Reported Error: " + e.getMessage()
                            ).send();

                            logger.error("Error while submitting request", e);
                        }
                );
            });
            
            case "message" -> event.deferReply(true).queue(hook -> {
                if(args.length < 4){
                    CommandUtil.EmbedReply.from(hook)
                        .error("Invalid Modal data. Expected `4+` arguments but received `" + args.length + "`!")
                        .send();
                    return;
                }
                
                TextChannel channel = guild.getTextChannelById(args[2]);
                if(channel == null){
                    CommandUtil.EmbedReply.from(hook).error("Received invalid Text Channel.").send();
                    return;
                }
                
                String text = value(event, "message");
                if(text == null || text.isEmpty()){
                    CommandUtil.EmbedReply.from(hook).error("Received invalid Message to sent/edit.").send();
                    return;
                }
                
                if(!channel.canTalk()){
                    CommandUtil.EmbedReply.from(hook)
                        .error("I lack the permission to see and/or write in " + channel.getAsMention() + ".")
                        .send();
                    return;
                }
                
                boolean asEmbed = Boolean.parseBoolean(args[3]);
                
                if(args[1].equals("post")){
                    if(asEmbed){
                        channel.sendMessageEmbeds(CommandUtil.getEmbed().setDescription(text).build()).queue(
                            message -> sendConfirmation(hook, message, false),
                            e -> CommandUtil.EmbedReply.from(hook)
                                .error("Unable to sent message. Reason: " + e.getMessage())
                                .send()
                        );
                    }else{
                        channel.sendMessage(text).queue(
                            message -> sendConfirmation(hook, message, false),
                            e -> CommandUtil.EmbedReply.from(hook)
                                .error("Unable to sent message. Reason: " + e.getMessage())
                                .send()
                        );
                    }
                }else if(args[1].equals("edit")){
                    if(args.length == 4){
                        CommandUtil.EmbedReply.from(hook)
                            .error("Received invalid Modal data. Expected `>4` but got `=4`")
                            .send();
                        return;
                    }
                    
                    long messageId;
                    try{
                        messageId = Long.parseLong(args[4]);
                    }catch(NumberFormatException ex){
                        messageId = -1L;
                    }
                    
                    if(messageId == -1L){
                        CommandUtil.EmbedReply.from(hook)
                            .error("Received invalid message ID `" + args[4] + "`.")
                            .send();
                        return;
                    }
                    
                    channel.retrieveMessageById(messageId).queue(
                        message -> {
                            if(asEmbed){
                                message.editMessageEmbeds(CommandUtil.getEmbed().setDescription(text).build()).setReplace(true).queue(
                                    m -> sendConfirmation(hook, m, true),
                                    e -> CommandUtil.EmbedReply.from(hook)
                                        .error("Unable to edit message. Reason: " + e.getMessage())
                                        .send()
                                );
                            }else{
                                message.editMessage(text).setReplace(true).queue(
                                    m -> sendConfirmation(hook, m, true),
                                    e -> CommandUtil.EmbedReply.from(hook)
                                        .error("Unable to edit message. Reason: " + e.getMessage())
                                        .send()
                                );
                            }
                        }
                    );
                }else{
                    CommandUtil.EmbedReply.from(hook)
                        .error("Received Unknown Message type: `" + args[1] + "`.")
                        .send();
                }
            });
            
            case "deny_application" -> event.deferReply(true).queue(hook -> {
                if(args.length == 1){
                    CommandUtil.EmbedReply.from(hook).error("Received invalid Deny Application modal!").send();
                    return;
                }
                
                long messageId;
                try{
                    messageId = Long.parseLong(args[1]);
                }catch(NumberFormatException ex){
                    messageId = -1L;
                }
                
                if(messageId == -1L){
                    CommandUtil.EmbedReply.from(hook).error("Received invalid message ID: " + args[1]).send();
                    return;
                }
                
                String reason = value(event, "reason");
                
                if(reason == null || reason.isEmpty())
                    reason = "*No reason provided*";
                
                ApplicationHandler.handle(this.bot, hook, guild, messageId, reason, false);
            });
            
            default -> CommandUtil.EmbedReply.from(event)
                .error("Received Modal with unknown ID `" + event.getModalId() + "`.")
                .send();
        }
    }
    
    private void sendConfirmation(InteractionHook hook, Message message, boolean edit){
        CommandUtil.EmbedReply.from(hook)
            .success(String.format("[%s](%s)", edit ? "Message edited!" : "Message sent!", message.getJumpUrl()))
            .send();
        
        logger.info("[Message] User {} {} a Message as the Bot.", hook.getInteraction().getUser().getEffectiveName(), edit ? "edited" : "sent");
    }
    
    private String value(ModalInteractionEvent event, String id){
        ModalMapping value = event.getValue(id);
        if(value == null)
            return null;
        
        return value.getAsString();
    }
}
